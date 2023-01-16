package br.usp.inova.c4ai.blab.internal.network;

/**
 * Represents a line in an HTTP request or response.
 *
 * @param key   the key
 * @param value the value
 */
public record HeaderEntry(String key, String value) {

}
