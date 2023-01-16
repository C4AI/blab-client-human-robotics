package br.usp.inova.c4ai.blab.blab;

import br.usp.inova.c4ai.blab.internal.JSONFormat;
import br.usp.inova.c4ai.blab.internal.network.Cookie;
import br.usp.inova.c4ai.blab.internal.network.HeaderEntry;
import br.usp.inova.c4ai.blab.internal.network.Network;
import br.usp.inova.c4ai.blab.internal.network.ResponseCallback;
import br.usp.inova.c4ai.blab.internal.network.WebSocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BLABClient {

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

    public void startConversation(String nickname, List<String> bots, String conversationName, Consumer<String> callback) {
        ConversationCreationRequestData body = new ConversationCreationRequestData(nickname, bots, conversationName);

        network.post(baseURL + "/conversations/", "application/json",
                json.toJson(body).getBytes(StandardCharsets.UTF_8),
                new ResponseCallback() {
                    @Override
                    public void onFailure(IOException e) {
                        e.printStackTrace();
                        callback.accept(null);
                    }

                    @Override
                    public void onResponse(br.usp.inova.c4ai.blab.internal.network.Response response) {
                        if (!response.success()) {
                            System.err.println("Request failed: " + Arrays.toString(response.body()));
                            callback.accept(null);
                            return;
                        }
                        ConversationCreationResponseData d = json.fromJson(response.string(), ConversationCreationResponseData.class);
                        List<Cookie> cookies = response.cookies();
                        Optional<Cookie> sessionCookie = cookies.stream().filter(c -> "sessionid".equals(c.name())).findFirst();
                        if (sessionCookie.isEmpty()) {
                            System.err.println("Missing cookie");
                            callback.accept(null);
                            return;
                        }
                        sessionId = sessionCookie.get().value();
                        createWebSocket(d.id, sessionId);
                        callback.accept(d.id);
                    }
                });
    }

    private void createWebSocket(String conversationId, String sessionId) {
        String url = wsBaseURL + "/" + conversationId + "/";
        var header = List.of(new HeaderEntry("Cookie", "sessionid=" + sessionId));
        ws = network.newWebSocket(url, header, new br.usp.inova.c4ai.blab.internal.network.WebSocketListener() {

            protected void onMessage(String text) {
                super.onMessage(text);
                WebSocketMessageData d = json.fromJson(text, WebSocketMessageData.class);
                System.out.println(d);
                if (d.message != null && "T".equals(d.message.type) && !d.message.sentByHuman) {
                    callback.accept(d.message.text);
                }
            }
        });
    }

    public void sendMessage(String text) {
        ws.send(json.toJson(Map.of("type", "T", "local_id", UUID.randomUUID().toString().replaceAll("-", ""), "text", text)));
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

}
