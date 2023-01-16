package br.usp.inova.c4ai.blab.internal.network;

import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Handles request responses.
 */
public interface Response {

    /**
     * Wraps an OkHttp {@link okhttp3.Response} instance.
     *
     * @param response the {@link okhttp3.Response} object
     * @return the wrapped object
     */
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

    /**
     * Returns {@code true} if and only if the request completed successfully.
     *
     * @return {@code true} if the request completed successfully, {@code false} otherwise
     */
    boolean success();

    /**
     * Returns the response code.
     *
     * @return the response code
     */
    int code();

    /**
     * Returns the response headers.
     *
     * @return the response headers mapping each name to a list of values
     */
    Map<String, List<String>> headers();

    /**
     * Returns the response cookies.
     *
     * @return the list of cookies received in the response
     */
    List<Cookie> cookies();

    /**
     * Returns the response body.
     *
     * @return the response body as a byte array
     */
    byte[] body();

    /**
     * Returns the text response body.
     *
     * @return the response body as a string
     */
    String bodyAsString();
}
