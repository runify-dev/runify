package com.run.ai.openai;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny replacement for the official SDK JsonValue used by additional properties.
 */
public final class JsonValue {

    private final Object value;

    private JsonValue(Object value) {
        this.value = normalize(value);
    }

    public static JsonValue from(Object value) {
        if (value instanceof JsonValue jsonValue) {
            return jsonValue;
        }
        return new JsonValue(value);
    }

    public Object raw() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (type == String.class) {
            if (value instanceof JsonObject jsonObject) {
                return (T) jsonObject.encode();
            }
            if (value instanceof JsonArray jsonArray) {
                return (T) jsonArray.encode();
            }
            return (T) String.valueOf(value);
        }
        if (type == Integer.class || type == int.class) {
            if (value instanceof Number number) {
                return (T) Integer.valueOf(number.intValue());
            }
            return (T) Integer.valueOf(String.valueOf(value));
        }
        if (type == Long.class || type == long.class) {
            if (value instanceof Number number) {
                return (T) Long.valueOf(number.longValue());
            }
            return (T) Long.valueOf(String.valueOf(value));
        }
        if (type == Double.class || type == double.class) {
            if (value instanceof Number number) {
                return (T) Double.valueOf(number.doubleValue());
            }
            return (T) Double.valueOf(String.valueOf(value));
        }
        if (type == Boolean.class || type == boolean.class) {
            if (value instanceof Boolean bool) {
                return (T) bool;
            }
            return (T) Boolean.valueOf(String.valueOf(value));
        }
        throw new IllegalArgumentException("Unsupported conversion to " + type.getName());
    }

    public Object toJsonCompatibleValue() {
        return unwrap(value);
    }

    private static Object normalize(Object input) {
        if (input instanceof JsonValue jsonValue) {
            return jsonValue.raw();
        }
        if (input instanceof Map<?, ?> map) {
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    jsonObject.put(String.valueOf(entry.getKey()), unwrap(normalize(entry.getValue())));
                }
            }
            return jsonObject;
        }
        if (input instanceof Collection<?> collection) {
            JsonArray jsonArray = new JsonArray();
            for (Object item : collection) {
                jsonArray.add(unwrap(normalize(item)));
            }
            return jsonArray;
        }
        return input;
    }

    private static Object unwrap(Object input) {
        if (input instanceof JsonValue jsonValue) {
            return unwrap(jsonValue.raw());
        }
        if (input instanceof JsonObject jsonObject) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, Object> entry : jsonObject) {
                copy.put(entry.getKey(), unwrap(entry.getValue()));
            }
            return copy;
        }
        if (input instanceof JsonArray jsonArray) {
            JsonArray copy = new JsonArray();
            for (Object item : jsonArray) {
                copy.add(unwrap(item));
            }
            return copy;
        }
        if (input instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    copy.put(String.valueOf(entry.getKey()), unwrap(entry.getValue()));
                }
            }
            return copy;
        }
        if (input instanceof Collection<?> collection) {
            JsonArray copy = new JsonArray();
            for (Object item : collection) {
                copy.add(unwrap(item));
            }
            return copy;
        }
        return input;
    }

    @Override
    public String toString() {
        if (value instanceof JsonObject jsonObject) {
            return jsonObject.encode();
        }
        if (value instanceof JsonArray jsonArray) {
            return jsonArray.encode();
        }
        return String.valueOf(value);
    }
}
