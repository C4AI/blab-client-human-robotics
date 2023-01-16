package br.usp.inova.c4ai.blab.internal;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Contains methods to convert Java objects into JSON strings and vice versa.
 * <p>
 * Note that this class basically wraps Google GSON usage.
 * <p>
 * IMPORTANT: this class is intended for internal use only, and its API can change
 * at any time. Other applications should use GSON directly (or similar libraries).
 */
public class JSONFormat {

    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();


    /**
     * Converts a JSON string into an object of the specified class.
     *
     * @param json     a JSON string
     * @param classOfT the class of the object to be returned
     * @param <T>      the type of the object to be returned
     * @return an instance of the given class containing data in the input string
     */
    public <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * Converts an object into a JSON string.
     *
     * @param object the object to be converted
     * @return the JSON representation of such object
     */
    public String toJson(Object object) {
        return gson.toJson(object);
    }


}
