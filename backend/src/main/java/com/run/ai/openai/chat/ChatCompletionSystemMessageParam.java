package com.run.ai.openai.chat;

import io.vertx.core.json.JsonObject;

/** System message builder. */
public final class ChatCompletionSystemMessageParam {

    private final JsonObject body;

    private ChatCompletionSystemMessageParam(JsonObject body) {
        this.body = body;
    }

    public static Builder builder() {
        return new Builder();
    }

    JsonObject toJsonObject() {
        return body.copy();
    }

    public static final class Builder {
        private final JsonObject body = new JsonObject().put("role", "system");

        public Builder content(String content) {
            body.put("content", content);
            return this;
        }

        public Builder putAdditionalProperty(String key, Object value) {
            body.put(key, value);
            return this;
        }

        public ChatCompletionSystemMessageParam build() {
            return new ChatCompletionSystemMessageParam(body.copy());
        }
    }
}
