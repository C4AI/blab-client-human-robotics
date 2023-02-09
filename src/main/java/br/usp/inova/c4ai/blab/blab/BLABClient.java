package br.usp.inova.c4ai.blab.blab;

import br.usp.inova.c4ai.blab.internal.JSONFormat;
import br.usp.inova.c4ai.blab.internal.network.Cookie;
import br.usp.inova.c4ai.blab.internal.network.HeaderEntry;
import br.usp.inova.c4ai.blab.internal.network.Network;
import br.usp.inova.c4ai.blab.internal.network.Response;
import br.usp.inova.c4ai.blab.internal.network.ResponseCallback;
import br.usp.inova.c4ai.blab.internal.network.WebSocket;
import br.usp.inova.c4ai.blab.internal.network.WebSocketListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * Handles bidirectional communication with BLAB Controller.
 */
public class BLABClient {

    /**
     * Pattern that matches a dash ("-"). Used to remove dashes from UUID4 strings.
     */
    private static final Pattern DASH = Pattern.compile("-");

    /**
     * Class logger.
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * URL of the HTTP(S) chat API provided by BLAB Controller server.
     */
    private final String baseURL;

    /**
     * Instance of a JSON parser and serializer.
     */
    private final JSONFormat json = new JSONFormat();

    /**
     * URL of the WebSocket starter provided by BLAB Controller.
     */
    private final String wsBaseURL;

    /**
     * Function that is called whenever a message is received.
     */
    private final Consumer<String> callback;

    /**
     * HTTP and WebSocket handler.
     */
    private final Network network;

    /**
     * WebSocket connection.
     */
    private WebSocket ws;

    /**
     * ID of the session, used to authenticate requests.
     */
    private String sessionId;

    /**
     * Initializes an instance.
     *
     * @param serverAddress   URL of the HTTP(S) chat API provided by BLAB Controller server
     * @param wsServerAddress URL of the WebSocket starter provided by BLAB Controller
     * @param callback        function that is called whenever a message is received
     */
    public BLABClient(String serverAddress, String wsServerAddress, Consumer<String> callback) {
        this.baseURL = serverAddress;
        this.wsBaseURL = wsServerAddress;
        this.callback = callback;
        this.network = new Network();
    }

    /**
     * Creates a new conversation.
     *
     * @param nickname         User's nickname
     * @param bots             list of bot names to include in the conversation
     * @param conversationName name of the conversation
     * @param callbackFunction function that is called with the conversation id as its only argument as soon as the
     *                         conversation starts, or {@code null} if the request fails
     */
    public void startConversation(String nickname, List<String> bots, String conversationName, Consumer<String> callbackFunction) {
        ConversationCreationRequestData body = new ConversationCreationRequestData(nickname, bots, conversationName);
        network.post(baseURL + "/conversations/", "application/json",
                json.toJson(body).getBytes(StandardCharsets.UTF_8),
                new StartConversationCallback(callbackFunction));
    }

    /**
     * Create a WebSocket connection with BLAB Controller.
     *
     * @param conversationId ID of the conversation
     */
    private void createWebSocket(String conversationId) {
        String url = wsBaseURL + "/" + conversationId + "/";
        var header = List.of(new HeaderEntry("Cookie", "sessionid=" + sessionId));
        ws = network.newWebSocket(url, header, new WSListener());
    }

    /**
     * Send a message from the user to BLAB controller.
     *
     * @param text the user message
     */
    public void sendMessage(String text) {
        String localId = DASH.matcher(UUID.randomUUID().toString()).replaceAll("");
        String contents = json.toJson(Map.of("type", "T", "local_id", localId, "text", text));
        if (!ws.send(contents))
            logger.error("Failed to send message: \"{}\"", text);
    }

    /**
     * Represents a participant type.
     */
    enum ParticipantType {
        /**
         * A bot participant.
         */
        BOT("B"),

        /**
         * A human participant.
         */
        HUMAN("H");

        /**
         * Single-letter participant type code.
         */
        final String code;

        ParticipantType(String code) {
            this.code = code;
        }

        /**
         * Returns an instance given a code.
         *
         * @param code "H" for human, "B" for bot
         * @return a ParticipantType instance if {@code code} is valid, throws {@link IllegalArgumentException}
         * otherwise
         */
        public static Enum<ParticipantType> fromCode(String code) {
            return switch (code) {
                case "H" -> HUMAN;
                case "B" -> BOT;
                default -> throw new IllegalArgumentException("Invalid participant type: " + code);
            };
        }

    }

    /**
     * Contains data sent in a conversation creation request.
     *
     * @param nickname         user's nickname
     * @param bots             list of bots to include in a conversation
     * @param conversationName name of the conversation
     */
    private record ConversationCreationRequestData(String nickname, List<String> bots, String conversationName) {
    }

    /**
     * Contains data received by the server after a conversation is created.
     *
     * @param id              ID of the conversation
     * @param name            conversation name
     * @param myParticipantId ID of the user's participant
     * @param participants    list of participants in the conversation
     */
    private record ConversationCreationResponseData(String id, String name, String myParticipantId,
                                                    List<Participant> participants) {


    }

    /**
     * Represents a participant in a conversation.
     *
     * @param id   ID of the participant
     * @param name participant's name
     * @param type type of the participant
     */
    private record Participant(String id, String name, ParticipantType type) {
    }

    /**
     * Represents data received from BLAB Controller via WebSocket.
     *
     * @param message
     * @param state
     */
    record WebSocketMessageData(Message message, State state) {

        /**
         * Represents a message received from BLAB Controller via WebSocket.
         *
         * @param type               message type ("T" for text, "S" for system; other types aren't supported)
         * @param time               when the message was sent (ISO-8601 format)
         * @param id                 message ID
         * @param text               message text
         * @param sentByHuman        whether the message was sent by a person
         * @param event              event type (for system messages)
         * @param additionalMetadata additional information (for system messages)
         * @param options            list of options that the user can choose (currently not supported by this client)
         */
        protected record Message(String type, String time, String id, String text, Boolean sentByHuman, String event,
                                 Map<String, String> additionalMetadata, List<String> options) {
        }

        /**
         * Represents a status notification received from BLAB Controller via WebSocket.
         *
         * @param participants current list of participants in the conversation
         */
        protected record State(List<Participant> participants) {
        }
    }

    /**
     * Handles the result of requesting the creation of a conversation.
     */
    private final class StartConversationCallback implements ResponseCallback {

        /**
         * Function that is called when the result of requesting the start of a new conversation is known.
         * The conversation id is passed as its only argument, or {@code null} in case it fails.
         */
        private final Consumer<String> callbackFunction;

        private StartConversationCallback(Consumer<String> callbackFunction) {
            this.callbackFunction = callbackFunction;
        }

        @Override
        public void onFailure(IOException e) {
            logger.error("Failed to create a conversation", e);
            callbackFunction.accept(null);
        }

        @Override
        public void onResponse(Response response) {
            if (!response.success()) {
                logger.error("Failed to create a conversation. Code {}. {}", response.code(), Arrays.toString(response.body()));
                callbackFunction.accept(null);
                return;
            }
            ConversationCreationResponseData responseData = json.fromJson(response.bodyAsString(), ConversationCreationResponseData.class);
            List<Cookie> cookies = response.cookies();
            Optional<Cookie> sessionCookie = cookies.stream().filter(cookie -> "sessionid".equals(cookie.name())).findFirst();
            if (sessionCookie.isEmpty()) {
                logger.error("Missing cookie");
                callbackFunction.accept(null);
                return;
            }
            sessionId = sessionCookie.get().value();
            createWebSocket(responseData.id());
            callbackFunction.accept(responseData.id());
        }
    }

    /**
     * Listens for WebSocket messages.
     */
    private class WSListener extends WebSocketListener {

        /**
         * Calls the callback function whenever a bot message is received.
         */
        protected void onMessage(String text) {
            super.onMessage(text);
            WebSocketMessageData messageData = json.fromJson(text, WebSocketMessageData.class);
            logger.debug("Message received from BLAB Controller: {}", messageData);
            if (messageData.message() != null && "T".equals(messageData.message().type) && !messageData.message().sentByHuman())
                callback.accept(messageData.message().text());
        }
    }
}
