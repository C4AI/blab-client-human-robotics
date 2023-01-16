package br.usp.inova.c4ai.blab.internal.network;

public abstract class WebSocketListener {

    protected void onOpen(Response response) {
    }

    protected void onMessage(String text) {
    }

    protected void onClosing(int code, String reason) {
    }

    protected void onClosed(int code, String reason) {
    }

    protected void onFailure(Throwable t, Response response) {
    }

}
