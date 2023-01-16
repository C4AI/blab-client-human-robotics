package br.usp.inova.c4ai.blab.internal.network;

import java.io.IOException;

public interface ResponseCallback {

    void onFailure(IOException e);

    void onResponse(Response response);
}
