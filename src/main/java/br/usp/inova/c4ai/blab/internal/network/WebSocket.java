package br.usp.inova.c4ai.blab.internal.network;

/**
 * Handles data sent via a WebSocket connection.
 */
public interface WebSocket {

    /**
     * Wraps an OkHttp {@link okhttp3.WebSocket} instance.
     *
     * @param ws the OkHttp object
     * @return the wrapped object
     */
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

    /**
     * Send contents through this connection.
     *
     * @param text the text to be sent
     * @return whether the request was completed successfully
     */
    boolean send(String text);

    /**
     * Closes the connection.
     *
     * @param code   the closing code
     * @param reason the reason for closing
     * @return whether the request was completed successfully
     */
    boolean close(int code, String reason);
}
