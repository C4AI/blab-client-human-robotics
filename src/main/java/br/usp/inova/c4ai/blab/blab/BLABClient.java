package br.usp.inova.c4ai.blab.blab;

import br.usp.inova.c4ai.blab.internal.JSONFormat;
import br.usp.inova.c4ai.blab.internal.network.Cookie;
import br.usp.inova.c4ai.blab.internal.network.HeaderEntry;
import br.usp.inova.c4ai.blab.internal.network.Network;
import br.usp.inova.c4ai.blab.internal.network.Response;
import br.usp.inova.c4ai.blab.internal.network.ResponseCallback;
import br.usp.inova.c4ai.blab.internal.network.WebSocket;
import br.usp.inova.c4ai.blab.internal.network.WebSocketListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class BLABClient {

    private static final Pattern DASH = Pattern.compile("-");
    private final String baseURL;

    private final JSONFormat json = new JSONFormat();
    private final String wsBaseURL;
    private final Consumer<String> callback;
    private final Network network;
    private WebSocket ws;
    private String sessionId;

    public BLABClient(String serverAddress, String wsServerAddress, Consumer<String> callback) {
        this.baseURL = serverAddress;
        this.wsBaseURL = wsServerAddress;
        this.callback = callback;
        this.network = new Network();
    }

    public void startConversation(String nickname, List<String> bots, String conversationName, Consumer<String> callbackFunction) {
        ConversationCreationRequestData body = new ConversationCreationRequestData(nickname, bots, conversationName);

        network.post(baseURL + "/conversations/", "application/json",
                json.toJson(body).getBytes(StandardCharsets.UTF_8),
                new StartConversationCallback(callbackFunction));
    }

    private void createWebSocket(String conversationId) {
        String url = wsBaseURL + "/" + conversationId + "/";
        var header = List.of(new HeaderEntry("Cookie", "sessionid=" + sessionId));
        ws = network.newWebSocket(url, header, new WSListener());
    }

    public void sendMessage(String text) {
        String localId = DASH.matcher(UUID.randomUUID().toString()).replaceAll("");
        String contents = json.toJson(Map.of("type", "T", "local_id", localId, "text", text));
        if (!ws.send(contents))
            System.err.format("Failed to send message: “%s”%n", text);
    }


    record ConversationCreationRequestData(String nickname, List<String> bots, String conversationName) {
    }

    record ConversationCreationResponseData(String id, String name, String myParticipantId,
                                            List<Participant> participants) {


    }

    record Participant(String id, String name, String type) {
    }

    record WebSocketMessageData(Message message, State state) {
        public record Message(String type, String time, String id, String text, Boolean sentByHuman, String event,
                              Map<String, String> additionalMetadata, List<String> options) {
        }

        public record State(List<Participant> participants) {
        }
    }

    private final class StartConversationCallback implements ResponseCallback {
        private final Consumer<String> callbackFunction;

        private StartConversationCallback(Consumer<String> callbackFunction) {
            this.callbackFunction = callbackFunction;
        }

        @Override
        public void onFailure(IOException e) {
            e.printStackTrace();
            callbackFunction.accept(null);
        }

        @Override
        public void onResponse(Response response) {
            if (!response.success()) {
                System.err.println("Request failed: " + Arrays.toString(response.body()));
                callbackFunction.accept(null);
                return;
            }
            ConversationCreationResponseData responseData = json.fromJson(response.bodyAsString(), ConversationCreationResponseData.class);
            List<Cookie> cookies = response.cookies();
            Optional<Cookie> sessionCookie = cookies.stream().filter(cookie -> "sessionid".equals(cookie.name())).findFirst();
            if (sessionCookie.isEmpty()) {
                System.err.println("Missing cookie");
                callbackFunction.accept(null);
                return;
            }
            sessionId = sessionCookie.get().value();
            createWebSocket(responseData.id());
            callbackFunction.accept(responseData.id());
        }
    }

    private class WSListener extends WebSocketListener {

        protected void onMessage(String text) {
            super.onMessage(text);
            WebSocketMessageData messageData = json.fromJson(text, WebSocketMessageData.class);
            System.out.println(messageData);
            if (null != messageData.message() && "T".equals(messageData.message().type) && !messageData.message().sentByHuman())
                callback.accept(messageData.message().text());
        }
    }
}
