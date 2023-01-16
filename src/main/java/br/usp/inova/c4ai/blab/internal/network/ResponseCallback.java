package br.usp.inova.c4ai.blab.internal.network;

import java.io.IOException;

/**
 * Handles request responses.
 */
public interface ResponseCallback {

    /**
     * Called when an exception occurs.
     *
     * @param e the exception
     */
    void onFailure(IOException e);

    /**
     * Called when a response is received from the server.
     *
     * @param response the response
     */
    void onResponse(Response response);
}
