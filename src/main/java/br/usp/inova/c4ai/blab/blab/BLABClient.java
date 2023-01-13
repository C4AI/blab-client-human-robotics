package br.usp.inova.c4ai.blab.blab;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class BLABClient {

    private final String baseURL;
    private final OkHttpClient http;

    private final JSONFormatWrapper json = new JSONFormatWrapper();
    private final String wsBaseURL;
    private final Consumer<String> callback;
    private WebSocket ws;
    private String sessionId;

    public BLABClient(String serverAddress, String wsServerAddress, Consumer<String> callback) {
        this.baseURL = serverAddress;
        this.wsBaseURL = wsServerAddress;
        this.http = new OkHttpClient();
        this.callback = callback;
    }

    public void startConversation(String nickname, List<String> bots, String conversationName, Consumer<String> callback) {
        ConversationCreationRequestData body = new ConversationCreationRequestData(nickname, bots, conversationName);
        Request request = new Request.Builder()
                .url(baseURL + "/conversations/")
                .post(RequestBody.create(json.toJson(body), MediaType.parse("application/json")))
                .build();
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                callback.accept(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (!response.isSuccessful() || responseBody == null) {
                    System.err.println("Request failed: " + responseBody);
                    callback.accept(null);
                    return;
                }
                ConversationCreationResponseData d = json.fromJson(responseBody.string(), ConversationCreationResponseData.class);
                List<Cookie> cookies = Cookie.parseAll(response.request().url(), response.headers());
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
        Request request = new Request.Builder().url(wsBaseURL + "/" + conversationId + "/").header("Cookie", "sessionid=" + sessionId).build();
        ws = http.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
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
