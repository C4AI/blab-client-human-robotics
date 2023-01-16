package br.usp.inova.c4ai.blab.internal.network;

public interface WebSocket {

    static WebSocket fromOkHttp3WebSocket(okhttp3.WebSocket ws) {
        return new WebSocket() {

            public boolean send(String text) {
                return ws.send(text);
            }

            public boolean close(int code, String reason) {
                return ws.close(code, reason);
            }
        };
    }

    boolean send(String text);

    boolean close(int code, String reason);
}