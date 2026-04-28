package com.run.ai.openai.chat;

import io.vertx.core.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal chat message param. Supports role/content and arbitrary extra fields.
 */
public final class ChatCompletionMessageParam {

    private final JsonObject body;

    private ChatCompletionMessageParam(JsonObject body) {
        this.body = body.copy();
    }

    public static ChatCompletionMessageParam ofUser(String content) {
        return new ChatCompletionMessageParam(new JsonObject().put("role", "user").put("content", content));
    }

    public static ChatCompletionMessageParam ofUser(ChatCompletionUserMessageParam param) {
        return new ChatCompletionMessageParam(param.toJsonObject());
    }

    public static ChatCompletionMessageParam ofSystem(String content) {
        return new ChatCompletionMessageParam(new JsonObject().put("role", "system").put("content", content));
    }

    public static ChatCompletionMessageParam ofSystem(ChatCompletionSystemMessageParam param) {
        return new ChatCompletionMessageParam(param.toJsonObject());
    }

    public static ChatCompletionMessageParam ofAssistant(String content) {
        return new ChatCompletionMessageParam(new JsonObject().put("role", "assistant").put("content", content));
    }

    public static ChatCompletionMessageParam ofAssistant(ChatCompletionAssistantMessageParam param) {
        return new ChatCompletionMessageParam(param.toJsonObject());
    }

    public static ChatCompletionMessageParam fromJsonObject(JsonObject body) {
        Objects.requireNonNull(body, "body cannot be null");
        return new ChatCompletionMessageParam(body);
    }

    public static ChatCompletionMessageParam fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map cannot be null");
        return new ChatCompletionMessageParam(new JsonObject(map));
    }

    public JsonObject toJsonObject() {
        return body.copy();
    }

    public String role() {
        return body.getString("role");
    }

    public Object content() {
        return body.getValue("content");
    }

    public Map<String, Object> asMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : body) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
