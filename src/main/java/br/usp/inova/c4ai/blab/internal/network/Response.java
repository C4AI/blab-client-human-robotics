package br.usp.inova.c4ai.blab.internal.network;

import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Response {

    static Response fromOkHttp3Response(okhttp3.Response response) {
        ResponseBody body = response.body();
        List<Cookie> cookies = okhttp3.Cookie.parseAll(response.request().url(), response.headers()).stream().map(cookie -> new Cookie(cookie.name(), cookie.value())).toList();
        return new Response(
        ) {
            @Override
            public boolean success() {
                return response.isSuccessful();
            }

            @Override
            public int code() {
                return response.code();
            }

            @Override
            public Map<String, List<String>> headers() {
                return response.headers().toMultimap();
            }

            @Override
            public List<Cookie> cookies() {
                return cookies;
            }

            @Override
            public byte[] body() {
                try {
                    return body != null ? body.bytes() : null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public String bodyAsString() {
                try {
                    return body != null ? body.string() : null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    boolean success();

    int code();

    Map<String, List<String>> headers();

    List<Cookie> cookies();

    byte[] body();

    String bodyAsString();
}
