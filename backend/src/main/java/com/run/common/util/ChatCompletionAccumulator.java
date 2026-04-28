package com.run.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletion;
import com.run.ai.openai.chat.ChatCompletionChunk;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  01:31}
 * {@code @Version 1.0}
 * {@code @注释: 适配自用轻量 openai chat sdk}
 */
public class ChatCompletionAccumulator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String id;
    private String model;
    private String finishReason;

    /**
     * 轻量 SDK 中 usage 直接使用 Vert.x JsonObject 表示。
     */
    private JsonObject usage;

    private final StringBuilder content = new StringBuilder();
    private final StringBuilder refusal = new StringBuilder();
    private final Map<Integer, AccumulatedToolCall> toolCalls = new TreeMap<>();

    /**
     * 需要收集的 additionalProperties key 列表，比如 reasoning_content。
     */
    private final Set<String> trackedKeys;

    /**
     * key -> StringBuilder 累加内容。
     */
    private final Map<String, StringBuilder> additionalProperties = new LinkedHashMap<>();

    public ChatCompletionAccumulator() {
        this.trackedKeys = Collections.emptySet();
    }

    public ChatCompletionAccumulator(Collection<String> trackedKeys) {
        if (trackedKeys == null || trackedKeys.isEmpty()) {
            this.trackedKeys = Collections.emptySet();
        } else {
            this.trackedKeys = new LinkedHashSet<>(trackedKeys);
            trackedKeys.forEach(k -> additionalProperties.put(k, new StringBuilder()));
        }
    }

    @Getter
    public static class AccumulatedToolCall {
        private int index;
        private String id;
        private String type;
        private String functionName;
        private final StringBuilder functionArguments = new StringBuilder();

        public String getFunctionArguments() {
            return functionArguments.toString();
        }

        @Override
        public String toString() {
            return "AccumulatedToolCall{" +
                    "index=" + index +
                    ", id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", functionName='" + functionName + '\'' +
                    ", functionArguments='" + functionArguments + '\'' +
                    '}';
        }
    }

    public static class AccumulatedResult {
        @Getter
        private final String id;
        @Getter
        private final String model;
        /**
         * stop           模型自然结束或命中 stop sequence
         * length         达到 max_tokens 限制被截断
         * tool_calls     模型调用了 tool
         * content_filter 内容被安全过滤器拦截
         * function_call  旧版函数调用（已废弃）
         * null           流式中间 chunk，尚未结束
         */
        @Getter
        private final String finishReason;
        @Getter
        private final String content;
        /**
         * 拒绝原因文本。
         */
        @Getter
        private final String refusal;
        @Getter
        private final List<AccumulatedToolCall> toolCalls;

        private final JsonObject usage;

        @Getter
        private final boolean isToolCall;
        @Getter
        private final boolean isLegacyFunctionCall;
        @Getter
        private final boolean isTextResponse;
        @Getter
        private final boolean isAborted;
        /**
         * 模型拒绝回答。
         */
        private final boolean isRefusal;

        /**
         * 流结束但没有收到 finish_reason，说明异常断流。
         */
        @Getter
        private final boolean isIncomplete;
        /**
         * arguments 是否合法 JSON。
         */
        @Getter
        private final Map<String, Boolean> toolCallArgumentsValid;

        /**
         * additionalProperties 收集结果 key -> 完整内容。
         */
        @Getter
        private final Map<String, String> additionalProperties;

        private AccumulatedResult(ChatCompletionAccumulator acc) {
            this.id = acc.id;
            this.model = acc.model;
            this.finishReason = acc.finishReason;
            this.content = acc.content.toString();
            this.refusal = acc.refusal.toString();
            this.toolCalls = Collections.unmodifiableList(new ArrayList<>(acc.toolCalls.values()));
            this.usage = acc.usage == null ? null : acc.usage.copy();

            this.isToolCall = "tool_calls".equals(finishReason) || !this.toolCalls.isEmpty();
            this.isLegacyFunctionCall = "function_call".equals(finishReason);
            this.isTextResponse = "stop".equals(finishReason);
            this.isAborted = "length".equals(finishReason) || "content_filter".equals(finishReason);
            this.isRefusal = !this.refusal.isEmpty();
            this.isIncomplete = finishReason == null;

            Map<String, Boolean> validMap = new LinkedHashMap<>();
            for (AccumulatedToolCall tc : this.toolCalls) {
                String key = tc.getFunctionName();
                if (key == null || key.isBlank()) {
                    key = String.valueOf(tc.getIndex());
                }
                validMap.put(key, isValidJson(tc.getFunctionArguments()));
            }
            this.toolCallArgumentsValid = Collections.unmodifiableMap(validMap);

            Map<String, String> snapshot = new LinkedHashMap<>();
            acc.additionalProperties.forEach((k, v) -> snapshot.put(k, v.toString()));
            this.additionalProperties = Collections.unmodifiableMap(snapshot);
        }

        public boolean isRefusal() {
            return isRefusal;
        }

        public Optional<String> getAdditionalProperty(String key) {
            return Optional.ofNullable(additionalProperties.get(key));
        }

        private static boolean isValidJson(String json) {
            if (json == null || json.isBlank()) {
                return false;
            }
            try {
                OBJECT_MAPPER.readTree(json);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public Optional<JsonObject> getUsage() {
            return Optional.ofNullable(usage == null ? null : usage.copy());
        }

        @Override
        public String toString() {
            return "AccumulatedResult{" +
                    "id='" + id + '\'' +
                    ", model='" + model + '\'' +
                    ", finishReason='" + finishReason + '\'' +
                    ", isToolCall=" + isToolCall +
                    ", isLegacyFunctionCall=" + isLegacyFunctionCall +
                    ", isTextResponse=" + isTextResponse +
                    ", isAborted=" + isAborted +
                    ", isRefusal=" + isRefusal +
                    ", isIncomplete=" + isIncomplete +
                    ", content='" + content + '\'' +
                    ", refusal='" + refusal + '\'' +
                    ", toolCalls=" + toolCalls +
                    ", toolCallArgumentsValid=" + toolCallArgumentsValid +
                    ", additionalProperties=" + additionalProperties +
                    ", usage=" + usage +
                    '}';
        }
    }

    public ChatCompletionAccumulator append(ChatCompletion completion) {
        if (completion == null) {
            return this;
        }

        if (this.id == null) {
            this.id = completion.id();
        }
        if (this.model == null) {
            this.model = completion.model();
        }

        completion.usage().ifPresent(u -> this.usage = u.copy());

        for (ChatCompletion.Choice choice : completion.choices()) {
            choice.finishReason().ifPresent(fr -> this.finishReason = fr);

            ChatCompletion.Message message = choice.message();

            message.content().ifPresent(content::append);
            message.refusal().ifPresent(refusal::append);
            message.toolCalls().ifPresent(this::appendToolCalls);

            collectAdditionalProperties(choice._additionalProperties());
            collectAdditionalProperties(message._additionalProperties());
        }

        collectAdditionalProperties(completion._additionalProperties());

        return this;
    }

    public ChatCompletionAccumulator append(ChatCompletionChunk chunk) {
        if (chunk == null) {
            return this;
        }

        if (this.id == null) {
            this.id = chunk.id();
        }
        if (this.model == null) {
            this.model = chunk.model();
        }

        chunk.usage().ifPresent(u -> this.usage = u.copy());

        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            choice.finishReason().ifPresent(fr -> this.finishReason = fr);

            ChatCompletionChunk.Delta delta = choice.delta();
            delta.content().ifPresent(content::append);
            delta.refusal().ifPresent(refusal::append);
            delta.toolCalls().ifPresent(this::appendToolCalls);

            collectAdditionalProperties(choice._additionalProperties());
            collectAdditionalProperties(delta._additionalProperties());
        }

        collectAdditionalProperties(chunk._additionalProperties());

        return this;
    }

    private void appendToolCalls(JsonArray calls) {
        if (calls == null || calls.isEmpty()) {
            return;
        }

        for (int i = 0; i < calls.size(); i++) {
            Object value = calls.getValue(i);
            if (value instanceof JsonObject obj) {
                appendToolCall(obj, i);
            }
        }
    }

    private void appendToolCall(JsonObject tc, int fallbackIndex) {
        int index = tc.getInteger("index", fallbackIndex);

        AccumulatedToolCall merged = toolCalls.computeIfAbsent(index, i -> {
            AccumulatedToolCall m = new AccumulatedToolCall();
            m.index = i;
            return m;
        });

        String id = tc.getString("id");
        if (merged.id == null && !isBlank(id)) {
            merged.id = id;
        }

        String type = tc.getString("type");
        if (merged.type == null && !isBlank(type)) {
            merged.type = type;
        }

        JsonObject function = tc.getJsonObject("function");
        if (function != null) {
            if (merged.type == null) {
                merged.type = "function";
            }

            String name = function.getString("name");
            if (merged.functionName == null && !isBlank(name)) {
                merged.functionName = name;
            }

            String arguments = function.getString("arguments");
            if (!isBlank(arguments)) {
                merged.functionArguments.append(arguments);
            }
        }

        JsonObject custom = tc.getJsonObject("custom");
        if (custom != null) {
            if (merged.type == null) {
                merged.type = "custom";
            }

            String name = custom.getString("name");
            if (merged.functionName == null && !isBlank(name)) {
                merged.functionName = name;
            }

            String input = custom.getString("input");
            if (!isBlank(input)) {
                merged.functionArguments.append(input);
            }
        }
    }

    private void collectAdditionalProperties(Map<String, JsonValue> properties) {
        if (trackedKeys.isEmpty() || properties == null || properties.isEmpty()) {
            return;
        }

        properties.forEach((k, v) -> {
            StringBuilder sb = additionalProperties.get(k);
            if (sb != null && !isNull(v)) {
                sb.append(jsonValueToText(v));
            }
        });
    }

    private static boolean isNull(JsonValue value) {
        return value == null || value.raw() == null;
    }

    private static String jsonValueToText(JsonValue value) {
        if (value == null || value.raw() == null) {
            return "";
        }
        Object raw = value.raw();
        if (raw instanceof String s) {
            return s;
        }
        return value.toString();
    }

    public AccumulatedResult complete() {
        return new AccumulatedResult(this);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
