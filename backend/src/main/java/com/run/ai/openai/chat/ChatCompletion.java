package com.run.ai.openai.chat;

import com.run.ai.openai.JsonValue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Non-streaming chat completion response. */
public final class ChatCompletion {

    private static final Set<String> KNOWN_KEYS = Set.of("id", "object", "created", "model", "choices", "usage");

    private final String id;
    private final String object;
    private final Long created;
    private final String model;
    private final List<Choice> choices;
    private final JsonObject usage;
    private final Map<String, JsonValue> additionalProperties;
    private final JsonObject raw;

    private ChatCompletion(String id,
                           String object,
                           Long created,
                           String model,
                           List<Choice> choices,
                           JsonObject usage,
                           Map<String, JsonValue> additionalProperties,
                           JsonObject raw) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.model = model;
        this.choices = List.copyOf(choices);
        this.usage = usage == null ? null : usage.copy();
        this.additionalProperties = Collections.unmodifiableMap(new LinkedHashMap<>(additionalProperties));
        this.raw = raw.copy();
    }

    public static ChatCompletion fromJson(String json) {
        return fromJsonObject(new JsonObject(json));
    }

    public static ChatCompletion fromJsonObject(JsonObject object) {
        List<Choice> choices = new ArrayList<>();
        JsonArray choiceArray = object.getJsonArray("choices", new JsonArray());
        for (Object item : choiceArray) {
            if (item instanceof JsonObject choiceObject) {
                choices.add(Choice.fromJsonObject(choiceObject));
            }
        }

        return new ChatCompletion(
                object.getString("id"),
                object.getString("object"),
                object.getLong("created"),
                object.getString("model"),
                choices,
                object.getJsonObject("usage"),
                JsonObjectSupport.additionalProperties(object, KNOWN_KEYS),
                object
        );
    }

    public String id() {
        return id;
    }

    public String object() {
        return object;
    }

    public Optional<Long> created() {
        return Optional.ofNullable(created);
    }

    public String model() {
        return model;
    }

    public List<Choice> choices() {
        return choices;
    }

    public Optional<JsonObject> usage() {
        return Optional.ofNullable(usage == null ? null : usage.copy());
    }

    public Map<String, JsonValue> _additionalProperties() {
        return additionalProperties;
    }

    public JsonObject raw() {
        return raw.copy();
    }

    public static final class Choice {
        private static final Set<String> KNOWN_KEYS = Set.of("index", "message", "finish_reason", "logprobs");

        private final Integer index;
        private final Message message;
        private final String finishReason;
        private final JsonValue logprobs;
        private final Map<String, JsonValue> additionalProperties;

        private Choice(Integer index,
                       Message message,
                       String finishReason,
                       JsonValue logprobs,
                       Map<String, JsonValue> additionalProperties) {
            this.index = index;
            this.message = message;
            this.finishReason = finishReason;
            this.logprobs = logprobs;
            this.additionalProperties = Collections.unmodifiableMap(new LinkedHashMap<>(additionalProperties));
        }

        static Choice fromJsonObject(JsonObject object) {
            return new Choice(
                    object.getInteger("index"),
                    Message.fromJsonObject(object.getJsonObject("message", new JsonObject())),
                    JsonObjectSupport.nullableString(object, "finish_reason"),
                    object.containsKey("logprobs") ? JsonValue.from(object.getValue("logprobs")) : null,
                    JsonObjectSupport.additionalProperties(object, KNOWN_KEYS)
            );
        }

        public Optional<Integer> index() {
            return Optional.ofNullable(index);
        }

        public Message message() {
            return message;
        }

        public Optional<String> finishReason() {
            return Optional.ofNullable(finishReason);
        }

        public Optional<JsonValue> logprobs() {
            return Optional.ofNullable(logprobs);
        }

        public Map<String, JsonValue> _additionalProperties() {
            return additionalProperties;
        }
    }

    public static final class Message {
        private static final Set<String> KNOWN_KEYS = Set.of("role", "content", "refusal", "tool_calls");

        private final String role;
        private final String content;
        private final String refusal;
        private final JsonArray toolCalls;
        private final Map<String, JsonValue> additionalProperties;
        private final JsonObject raw;

        private Message(String role,
                        String content,
                        String refusal,
                        JsonArray toolCalls,
                        Map<String, JsonValue> additionalProperties,
                        JsonObject raw) {
            this.role = role;
            this.content = content;
            this.refusal = refusal;
            this.toolCalls = toolCalls == null ? null : toolCalls.copy();
            this.additionalProperties = Collections.unmodifiableMap(new LinkedHashMap<>(additionalProperties));
            this.raw = raw.copy();
        }

        static Message fromJsonObject(JsonObject object) {
            return new Message(
                    object.getString("role"),
                    JsonObjectSupport.nullableString(object, "content"),
                    JsonObjectSupport.nullableString(object, "refusal"),
                    object.getJsonArray("tool_calls"),
                    JsonObjectSupport.additionalProperties(object, KNOWN_KEYS),
                    object
            );
        }

        public Optional<String> role() {
            return Optional.ofNullable(role);
        }

        public Optional<String> content() {
            return Optional.ofNullable(content);
        }

        public Optional<String> refusal() {
            return Optional.ofNullable(refusal);
        }

        public Optional<JsonArray> toolCalls() {
            return Optional.ofNullable(toolCalls == null ? null : toolCalls.copy());
        }

        public Map<String, JsonValue> _additionalProperties() {
            return additionalProperties;
        }

        public JsonObject raw() {
            return raw.copy();
        }
    }
}
