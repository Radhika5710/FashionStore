package com.fashionstore.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * JSON Utility class for JSON serialization/deserialization
 * Uses Gson library
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
    
    /**
     * Convert JSON string to object with Type
     */
    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
    
    /**
     * Get the Gson instance
     */
    public static Gson gson() {
        return gson;
    }
    
    /**
     * Convert JSON string to List
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        Type listType = new TypeToken<List<T>>(){}.getType();
        return gson.fromJson(json, listType);
    }
    
    /**
     * Convert JSON string to Map
     */
    public static Map<String, Object> fromJsonMap(String json) {
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(json, mapType);
    }
}
