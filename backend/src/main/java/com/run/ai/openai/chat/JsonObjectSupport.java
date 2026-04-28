package com.run.ai.openai.chat;

import com.run.ai.openai.JsonValue;
import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class JsonObjectSupport {

    private JsonObjectSupport() {
    }

    static Map<String, JsonValue> additionalProperties(JsonObject jsonObject, Set<String> knownKeys) {
        Map<String, JsonValue> result = new LinkedHashMap<>();
        if (jsonObject == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : jsonObject) {
            if (!knownKeys.contains(entry.getKey())) {
                result.put(entry.getKey(), JsonValue.from(entry.getValue()));
            }
        }
        return result;
    }

    static String nullableString(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.containsKey(key)) {
            return null;
        }
        Object value = jsonObject.getValue(key);
        return value == null ? null : String.valueOf(value);
    }
}
