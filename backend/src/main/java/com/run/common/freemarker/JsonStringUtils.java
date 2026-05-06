package com.run.common.freemarker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Array;
import java.util.*;

public class JsonStringUtils {

    public static String toJsonString(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(normalize(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof io.vertx.core.json.JsonObject jsonObject) {
            return normalize(jsonObject.getMap());
        }

        if (value instanceof io.vertx.core.json.JsonArray jsonArray) {
            return normalize(jsonArray.getList());
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return result;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : collection) {
                result.add(normalize(item));
            }
            return result;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(normalize(Array.get(value, i)));
            }
            return result;
        }

        return value;
    }
}