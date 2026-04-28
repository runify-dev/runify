package com.run.ai.openai.chat;

import com.run.ai.openai.JsonValue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal chat.completions.create request params.
 */
public final class ChatCompletionCreateParams {

    private final String model;
    private final List<ChatCompletionMessageParam> messages;
    private final Map<String, JsonValue> additionalBodyProperties;

    private ChatCompletionCreateParams(String model,
                                       List<ChatCompletionMessageParam> messages,
                                       Map<String, JsonValue> additionalBodyProperties) {
        this.model = model;
        this.messages = List.copyOf(messages);
        this.additionalBodyProperties = Collections.unmodifiableMap(new LinkedHashMap<>(additionalBodyProperties));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String model() {
        return model;
    }

    public List<ChatCompletionMessageParam> messages() {
        return messages;
    }

    public Map<String, JsonValue> additionalBodyProperties() {
        return additionalBodyProperties;
    }

    public JsonObject toJsonObject(boolean stream) {
        JsonObject body = new JsonObject();
        body.put("model", model);

        JsonArray messageArray = new JsonArray();
        for (ChatCompletionMessageParam message : messages) {
            messageArray.add(message.toJsonObject());
        }
        body.put("messages", messageArray);

        for (Map.Entry<String, JsonValue> entry : additionalBodyProperties.entrySet()) {
            body.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toJsonCompatibleValue());
        }

        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    public static final class Builder {
        private String model;
        private final List<ChatCompletionMessageParam> messages = new ArrayList<>();
        private final Map<String, JsonValue> additionalBodyProperties = new LinkedHashMap<>();

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatCompletionMessageParam> messages) {
            this.messages.clear();
            if (messages != null) {
                this.messages.addAll(messages);
            }
            return this;
        }

        public Builder addMessage(ChatCompletionMessageParam message) {
            this.messages.add(Objects.requireNonNull(message, "message cannot be null"));
            return this;
        }

        public Builder putAdditionalBodyProperty(String key, JsonValue value) {
            this.additionalBodyProperties.put(key, value);
            return this;
        }

        public Builder putAdditionalBodyProperty(String key, Object value) {
            this.additionalBodyProperties.put(key, JsonValue.from(value));
            return this;
        }

        public Builder temperature(Double temperature) {
            if (temperature != null) {
                putAdditionalBodyProperty("temperature", temperature);
            }
            return this;
        }

        public Builder topP(Double topP) {
            if (topP != null) {
                putAdditionalBodyProperty("top_p", topP);
            }
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            if (maxTokens != null) {
                putAdditionalBodyProperty("max_tokens", maxTokens);
            }
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            if (maxCompletionTokens != null) {
                putAdditionalBodyProperty("max_completion_tokens", maxCompletionTokens);
            }
            return this;
        }

        public ChatCompletionCreateParams build() {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model cannot be blank");
            }
            if (messages.isEmpty()) {
                throw new IllegalArgumentException("messages cannot be empty");
            }
            return new ChatCompletionCreateParams(model, messages, additionalBodyProperties);
        }
    }
}
