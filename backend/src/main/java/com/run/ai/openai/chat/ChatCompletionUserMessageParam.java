package com.run.ai.openai.chat;

import io.vertx.core.json.JsonObject;

/** User message builder. */
public final class ChatCompletionUserMessageParam {

    private final JsonObject body;

    private ChatCompletionUserMessageParam(JsonObject body) {
        this.body = body;
    }

    public static Builder builder() {
        return new Builder();
    }

    JsonObject toJsonObject() {
        return body.copy();
    }

    public static final class Builder {
        private final JsonObject body = new JsonObject().put("role", "user");

        public Builder content(String content) {
            body.put("content", content);
            return this;
        }

        public Builder content(Object content) {
            body.put("content", content);
            return this;
        }

        public Builder putAdditionalProperty(String key, Object value) {
            body.put(key, value);
            return this;
        }

        public ChatCompletionUserMessageParam build() {
            return new ChatCompletionUserMessageParam(body.copy());
        }
    }
}
