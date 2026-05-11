package com.run.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletion;
import com.run.ai.openai.chat.ChatCompletionChunk;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

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

    private JsonObject usage;

    private final StringBuilder content = new StringBuilder();
    private final StringBuilder refusal = new StringBuilder();
    private final Map<Integer, AccumulatedToolCall> toolCalls = new TreeMap<>();

    private final Set<String> trackedKeys;
    private final Map<String, StringBuilder> additionalProperties = new LinkedHashMap<>();

    // ---------- 增量字段提取配置 ----------

    /**
     * 增量回调：只对 streamingFilter 命中的 tool_call 启用 JsonStringFieldsStreamExtractor。
     */
    private Predicate<AccumulatedToolCall> streamingFilter;
    private Set<String> argumentFieldsToExtract;
    private BiConsumer<AccumulatedToolCall, Map<String, String>> onArgumentFieldDelta;

    // ---------- 完整 arguments 回调配置 ----------

    /**
     * 完成回调：在 arguments 累加完整（JSON 合法）后触发一次。
     */
    private Predicate<AccumulatedToolCall> completeFilter;
    private BiConsumer<AccumulatedToolCall, String> onArgumentComplete;

    /**
     * 已经触发过 complete 回调的 tool_call index，防止重复触发。
     */
    private final Set<Integer> completedToolCalls = new HashSet<>();

    // ---------- id 重映射配置 ----------

    private boolean remapToolCallIdToUuid7 = true;
    private final Map<String, String> toolCallIdMapping = new HashMap<>();

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

    public ChatCompletionAccumulator remapToolCallIdToUuid7() {
        this.remapToolCallIdToUuid7 = true;
        return this;
    }

    // ==================== 增量回调注册 API ====================

    /**
     * 完整版增量回调注册。
     *
     * @param filter          只对哪些 tool_call 启用流式增量提取（按 functionName 等过滤）
     * @param fieldsToExtract 要提取的字段名（null/空 = 所有顶层字符串字段）
     * @param callback        (toolCall, 字段增量Map) -> void
     */
    public ChatCompletionAccumulator onArgumentFieldDelta(
            Predicate<AccumulatedToolCall> filter,
            Set<String> fieldsToExtract,
            BiConsumer<AccumulatedToolCall, Map<String, String>> callback) {
        this.streamingFilter = filter;
        this.argumentFieldsToExtract = fieldsToExtract;
        this.onArgumentFieldDelta = callback;
        return this;
    }

    /**
     * 便捷重载：只按函数名集合过滤。
     */
    public ChatCompletionAccumulator onArgumentFieldDelta(
            Set<String> streamingFunctionNames,
            Set<String> fieldsToExtract,
            BiConsumer<AccumulatedToolCall, Map<String, String>> callback) {
        Predicate<AccumulatedToolCall> filter = tc ->
                streamingFunctionNames != null
                        && tc.getFunctionName() != null
                        && streamingFunctionNames.contains(tc.getFunctionName());
        return onArgumentFieldDelta(filter, fieldsToExtract, callback);
    }

    /**
     * 便捷重载：所有 tool_call 都走增量，提取所有顶层字符串字段。
     */
    public ChatCompletionAccumulator onArgumentFieldDelta(
            BiConsumer<AccumulatedToolCall, Map<String, String>> callback) {
        return onArgumentFieldDelta((Predicate<AccumulatedToolCall>) null, null, callback);
    }

    // ==================== 完成回调注册 API ====================

    /**
     * 在 arguments 累加完整（JSON 合法）后触发一次的回调。
     * <p>
     * 触发时机：每收到新增的 arguments 后检测 JSON 合法性；一旦合法立即触发并标记，
     * 后续即使再来 chunk 也不会重复触发。
     *
     * @param filter   只对哪些 tool_call 触发完成回调
     * @param callback (toolCall, 完整 arguments JSON 字符串) -> void
     */
    public ChatCompletionAccumulator onArgumentComplete(
            Predicate<AccumulatedToolCall> filter,
            BiConsumer<AccumulatedToolCall, String> callback) {
        this.completeFilter = filter;
        this.onArgumentComplete = callback;
        return this;
    }

    /**
     * 便捷重载：所有 tool_call 都触发完成回调。
     */
    public ChatCompletionAccumulator onArgumentComplete(
            BiConsumer<AccumulatedToolCall, String> callback) {
        return onArgumentComplete((Predicate<AccumulatedToolCall>) null, callback);
    }

    /**
     * 便捷重载：只对指定函数名集合触发完成回调。
     */
    public ChatCompletionAccumulator onArgumentComplete(
            Set<String> functionNames,
            BiConsumer<AccumulatedToolCall, String> callback) {
        Predicate<AccumulatedToolCall> filter = tc ->
                functionNames != null
                        && tc.getFunctionName() != null
                        && functionNames.contains(tc.getFunctionName());
        return onArgumentComplete(filter, callback);
    }

    // ==================== id 映射查询 ====================

    public Optional<String> findOriginalIdByUuid7(String uuid7) {
        if (uuid7 == null) return Optional.empty();
        return toolCallIdMapping.entrySet().stream()
                .filter(e -> uuid7.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public Optional<String> findUuid7ByOriginalId(String originalId) {
        if (originalId == null) return Optional.empty();
        return Optional.ofNullable(toolCallIdMapping.get(originalId));
    }

    public Map<String, String> getToolCallIdMapping() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(toolCallIdMapping));
    }

    // ==================== 内部数据结构 ====================

    @Getter
    public static class AccumulatedToolCall {
        private int index;
        private String id;
        private String originalId;
        private String type;
        private String functionName;
        private final StringBuilder functionArguments = new StringBuilder();

        private JsonStringFieldsStreamExtractor extractor;

        public String getFunctionArguments() {
            return functionArguments.toString();
        }

        JsonStringFieldsStreamExtractor getOrCreateExtractor(Set<String> fieldsToExtract) {
            if (extractor == null) {
                extractor = (fieldsToExtract == null || fieldsToExtract.isEmpty())
                        ? new JsonStringFieldsStreamExtractor()
                        : new JsonStringFieldsStreamExtractor(fieldsToExtract);
            }
            return extractor;
        }

        @Override
        public String toString() {
            return "AccumulatedToolCall{" +
                    "index=" + index +
                    ", id='" + id + '\'' +
                    ", originalId='" + originalId + '\'' +
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
        @Getter
        private final String finishReason;
        @Getter
        private final String content;
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
        private final boolean isRefusal;
        @Getter
        private final boolean isIncomplete;
        @Getter
        private final Map<String, Boolean> toolCallArgumentsValid;
        @Getter
        private final Map<String, String> additionalProperties;
        @Getter
        private final Map<String, String> toolCallIdMapping;

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

            this.toolCallIdMapping = Collections.unmodifiableMap(
                    new LinkedHashMap<>(acc.toolCallIdMapping));
        }

        public boolean isRefusal() {
            return isRefusal;
        }

        public Optional<String> getAdditionalProperty(String key) {
            return Optional.ofNullable(additionalProperties.get(key));
        }

        private static boolean isValidJson(String json) {
            if (json == null || json.isBlank()) return false;
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
                    ", toolCallIdMapping=" + toolCallIdMapping +
                    ", usage=" + usage +
                    '}';
        }
    }

    // ==================== append 入口 ====================

    public ChatCompletionAccumulator append(ChatCompletion completion) {
        if (completion == null) return this;

        if (this.id == null) this.id = completion.id();
        if (this.model == null) this.model = completion.model();

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
        if (chunk == null) return this;

        if (this.id == null) this.id = chunk.id();
        if (this.model == null) this.model = chunk.model();

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
        if (calls == null || calls.isEmpty()) return;

        for (int i = 0; i < calls.size(); i++) {
            Object value = calls.getValue(i);
            if (value instanceof JsonObject obj) {
                appendToolCall(obj, i);
            }
        }
    }

    private void appendToolCall(JsonObject tc, int fallbackIndex) {
        Integer rawIndex = tc.getInteger("index");
        int index = rawIndex != null ? rawIndex : fallbackIndex;

        AccumulatedToolCall merged = toolCalls.computeIfAbsent(index, i -> {
            AccumulatedToolCall m = new AccumulatedToolCall();
            m.index = i;
            return m;
        });

        String rawId = tc.getString("id");
        if (merged.originalId == null && !isBlank(rawId)) {
            merged.originalId = rawId;
            merged.id = resolveId(rawId);
        }

        String type = tc.getString("type");
        if (merged.type == null && !isBlank(type)) {
            merged.type = type;
        }

        JsonObject function = tc.getJsonObject("function");
        if (function != null && (merged.type == null || "function".equals(merged.type))) {
            if (merged.type == null) merged.type = "function";

            String name = function.getString("name");
            if (merged.functionName == null && !isBlank(name)) {
                merged.functionName = name;
            }

            String arguments = function.getString("arguments");
            if (!isEmpty(arguments)) {
                merged.functionArguments.append(arguments);
                emitArgumentDelta(merged, arguments);
                tryEmitArgumentComplete(merged);
            }
        }

        JsonObject custom = tc.getJsonObject("custom");
        if (custom != null && (merged.type == null || "custom".equals(merged.type))) {
            if (merged.type == null) merged.type = "custom";

            String name = custom.getString("name");
            if (merged.functionName == null && !isBlank(name)) {
                merged.functionName = name;
            }

            String input = custom.getString("input");
            if (!isEmpty(input)) {
                merged.functionArguments.append(input);
                emitArgumentDelta(merged, input);
                tryEmitArgumentComplete(merged);
            }
        }
    }

    private String resolveId(String originalId) {
        if (!remapToolCallIdToUuid7) return originalId;
        return toolCallIdMapping.computeIfAbsent(
                originalId, k -> CommonUtils.uuid7().toString());
    }

    /**
     * 触发增量字段回调（只对 streamingFilter 命中的 tool_call 启用）。
     */
    private void emitArgumentDelta(AccumulatedToolCall toolCall, String delta) {
        if (onArgumentFieldDelta == null) return;

        // 函数名/类型还没解析出来时，先不启用流式提取（避免给错的 extractor）
        if (toolCall.getFunctionName() == null) return;

        // 过滤：streamingFilter 为 null 表示对所有 tool_call 启用
        if (streamingFilter != null && !streamingFilter.test(toolCall)) {
            return;
        }

        Map<String, String> incremental = toolCall
                .getOrCreateExtractor(argumentFieldsToExtract)
                .feed(delta);

        if (incremental.isEmpty()) return;

        try {
            onArgumentFieldDelta.accept(toolCall, incremental);
        } catch (Exception ignored) {
            // 回调异常不影响累加流程
        }
    }

    /**
     * 检查 arguments 是否累加完整（JSON 合法）。
     * 一旦合法且通过 completeFilter，触发 onArgumentComplete 回调一次。
     */
    private void tryEmitArgumentComplete(AccumulatedToolCall toolCall) {
        if (onArgumentComplete == null) return;
        if (completedToolCalls.contains(toolCall.getIndex())) return;
        if (toolCall.getFunctionName() == null) return;

        // 过滤：completeFilter 为 null 表示对所有 tool_call 触发
        if (completeFilter != null && !completeFilter.test(toolCall)) {
            return;
        }

        String args = toolCall.getFunctionArguments();
        if (!isValidJson(args)) return;

        completedToolCalls.add(toolCall.getIndex());

        try {
            onArgumentComplete.accept(toolCall, args);
        } catch (Exception ignored) {
            // 回调异常不影响累加流程
        }
    }

    private static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void collectAdditionalProperties(Map<String, JsonValue> properties) {
        if (trackedKeys.isEmpty() || properties == null || properties.isEmpty()) return;

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
        if (value == null || value.raw() == null) return "";
        Object raw = value.raw();
        if (raw instanceof String s) return s;
        return value.toString();
    }

    public AccumulatedResult complete() {
        return new AccumulatedResult(this);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}