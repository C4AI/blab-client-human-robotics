package br.usp.inova.c4ai.blab.internal.network;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;

public class Network {

    private final OkHttpClient http = new OkHttpClient();

    public void post(String url, String mimeType, byte[] body, ResponseCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, MediaType.parse(mimeType)))
                .build();
        http.newCall(request).enqueue(new CreateConversationCallback(callback));
    }

    public WebSocket newWebSocket(String url, Iterable<HeaderEntry> headers, WebSocketListener listener) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        for (HeaderEntry h : headers)
            requestBuilder = requestBuilder.header(h.key(), h.value());
        Request request = requestBuilder.build();
        return WebSocket.fromOkHttp3WebSocket(http.newWebSocket(request, new CreateWebSocketListener(listener)));
    }


    private static final class CreateConversationCallback implements Callback {
        private final ResponseCallback callback;

        private CreateConversationCallback(ResponseCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(Call call, okhttp3.Response response) {
            callback.onResponse(Response.fromOkHttp3Response(response));
        }

        @Override
        public void onFailure(Call call, IOException e) {
            callback.onFailure(e);
        }

    }

    private static final class CreateWebSocketListener extends okhttp3.WebSocketListener {
        private final WebSocketListener listener;

        private CreateWebSocketListener(WebSocketListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            listener.onClosed(code, reason);
        }

        @Override
        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            listener.onClosing(code, reason);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
            super.onFailure(webSocket, t, response);
            listener.onFailure(t, Response.fromOkHttp3Response(response));
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            listener.onMessage(text);
        }

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, okhttp3.Response response) {
            super.onOpen(webSocket, response);
            listener.onOpen(Response.fromOkHttp3Response(response));
        }
    }
}
