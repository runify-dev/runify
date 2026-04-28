package com.run.ai.openai.chat;

import com.run.ai.openai.JsonValue;
import io.vertx.core.json.JsonArray;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Accumulates content/reasoning/refusal/tool_calls/finish_reason from streaming or non-streaming responses.
 */
public final class ChatCompletionAccumulator {

    private final StringBuilder content = new StringBuilder();
    private final StringBuilder refusal = new StringBuilder();
    private final Map<String, StringBuilder> additionalText = new LinkedHashMap<>();
    private final Map<String, Object> additionalLastValue = new LinkedHashMap<>();
    private final JsonArray toolCalls = new JsonArray();
    private String finishReason;

    public void append(ChatCompletionChunk chunk) {
        if (chunk == null) {
            return;
        }
        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            ChatCompletionChunk.Delta delta = choice.delta();

            delta.content().ifPresent(content::append);
            delta.refusal().ifPresent(refusal::append);
            delta.toolCalls().ifPresent(this::appendToolCalls);

            for (Map.Entry<String, JsonValue> entry : delta._additionalProperties().entrySet()) {
                appendAdditional(entry.getKey(), entry.getValue());
            }

            choice.finishReason().ifPresent(value -> this.finishReason = value);
        }
    }

    public void append(ChatCompletion completion) {
        if (completion == null) {
            return;
        }
        for (ChatCompletion.Choice choice : completion.choices()) {
            ChatCompletion.Message message = choice.message();

            message.content().ifPresent(content::append);
            message.refusal().ifPresent(refusal::append);
            message.toolCalls().ifPresent(this::appendToolCalls);

            for (Map.Entry<String, JsonValue> entry : message._additionalProperties().entrySet()) {
                appendAdditional(entry.getKey(), entry.getValue());
            }

            choice.finishReason().ifPresent(value -> this.finishReason = value);
        }
    }

    public AccumulatedResult complete() {
        Map<String, Object> additional = new LinkedHashMap<>(additionalLastValue);
        for (Map.Entry<String, StringBuilder> entry : additionalText.entrySet()) {
            additional.put(entry.getKey(), entry.getValue().toString());
        }
        return new AccumulatedResult(
                content.toString(),
                additional,
                refusal.toString(),
                refusal.length() > 0,
                toolCalls.copy(),
                finishReason
        );
    }

    private void appendAdditional(String key, JsonValue value) {
        if (key == null || value == null) {
            return;
        }
        Object raw = value.raw();
        if (raw instanceof String string) {
            additionalText.computeIfAbsent(key, ignored -> new StringBuilder()).append(string);
        } else if (raw != null) {
            additionalLastValue.put(key, value.toJsonCompatibleValue());
        }
    }

    private void appendToolCalls(JsonArray calls) {
        if (calls == null) {
            return;
        }
        for (Object call : calls) {
            toolCalls.add(call);
        }
    }

    public static final class AccumulatedResult {
        private final String content;
        private final Map<String, Object> additionalProperties;
        private final String refusal;
        private final boolean refusalFlag;
        private final JsonArray toolCalls;
        private final String finishReason;

        private AccumulatedResult(String content,
                                  Map<String, Object> additionalProperties,
                                  String refusal,
                                  boolean refusalFlag,
                                  JsonArray toolCalls,
                                  String finishReason) {
            this.content = content;
            this.additionalProperties = Collections.unmodifiableMap(new LinkedHashMap<>(additionalProperties));
            this.refusal = refusal;
            this.refusalFlag = refusalFlag;
            this.toolCalls = toolCalls == null ? new JsonArray() : toolCalls.copy();
            this.finishReason = finishReason;
        }

        public String getContent() {
            return content;
        }

        public Optional<Object> getAdditionalProperty(String key) {
            return Optional.ofNullable(additionalProperties.get(key));
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        public String getRefusal() {
            return refusal;
        }

        public boolean isRefusal() {
            return refusalFlag;
        }

        public JsonArray getToolCalls() {
            return toolCalls.copy();
        }

        public String getFinishReason() {
            return finishReason;
        }
    }
}
