package com.run.workflow;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/30  15:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class Tools {
    private static Gson gson = new GsonBuilder().create();
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Gson getGson() {
        return gson;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
