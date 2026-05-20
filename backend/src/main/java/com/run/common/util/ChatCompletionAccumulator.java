package com.run.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.chat.ChatCompletion;
import com.run.ai.openai.chat.ChatCompletionChunk;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

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
import java.util.function.Consumer;

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

    /**
     * 调用方传入的标识，用于将本次响应产生的所有 tool_calls 关联到同一次 AI 调用。
     * 构建上下文时可通过 {@link AccumulatedToolCall#getAiId()} 得到。
     */
    @Getter
    private final String aiId;

    private final StringBuilder content = new StringBuilder();
    private final StringBuilder refusal = new StringBuilder();
    private final Map<Integer, AccumulatedToolCall> toolCalls = new TreeMap<>();

    private final Set<String> trackedKeys;
    private final Map<String, StringBuilder> additionalProperties = new LinkedHashMap<>();

    // ---------- tool_call chunk 回调配置 ----------

    /**
     * 每收到一个有效的 tool_call arguments/input chunk，触发一次。
     * <p>
     * 只有 id / type 都已经解析到，且 functionName 在 expectedToolNames 白名单内时才触发回调。
     * 回调参数里的 index / id / originalId / type / functionName 每次都返回已知完整值；
     * functionArguments 只表示当前 chunk 的 arguments/input 片段，不包含之前已经收到的内容。
     */
    private Consumer<ToolCallChunk> onToolCallChunk;

    // ---------- 期望的 tool names 白名单 ----------

    private final Set<String> expectedToolNames;

    // ---------- reasoning 字段名配置 ----------

    /**
     * 不同模型的思考字段名不同：
     * OpenAI o 系列: "reasoning"
     * Claude: "thinking"
     * DeepSeek: "reasoning_content"
     * 按顺序优先匹配第一个非空值。
     */
    private List<String> reasoningKeys = List.of("reasoning", "thinking", "reasoning_content");

    // ---------- id 重映射配置 ----------

    private boolean remapToolCallIdToUuid7 = true;
    private final Map<String, String> toolCallIdMapping = new HashMap<>();

    public ChatCompletionAccumulator() {
        this.aiId = CommonUtils.uuid7().toString();
        this.trackedKeys = Collections.emptySet();
        this.expectedToolNames = Collections.emptySet();
    }

    public ChatCompletionAccumulator(String aiId) {
        this.aiId = aiId != null ? aiId : CommonUtils.uuid7().toString();
        this.trackedKeys = Collections.emptySet();
        this.expectedToolNames = Collections.emptySet();
    }

    public ChatCompletionAccumulator(Collection<String> trackedKeys) {
        this(null, trackedKeys, null);
    }

    public ChatCompletionAccumulator(String aiId, Collection<String> trackedKeys) {
        this(aiId, trackedKeys, null);
    }

    public ChatCompletionAccumulator(Collection<String> trackedKeys,
                                     Collection<String> expectedToolNames) {
        this(null, trackedKeys, expectedToolNames);
    }

    public ChatCompletionAccumulator(String aiId,
                                     Collection<String> trackedKeys,
                                     Collection<String> expectedToolNames) {
        this.aiId = aiId != null ? aiId : CommonUtils.uuid7().toString();

        if (trackedKeys == null || trackedKeys.isEmpty()) {
            this.trackedKeys = Collections.emptySet();
        } else {
            this.trackedKeys = new LinkedHashSet<>(trackedKeys);
            trackedKeys.forEach(k -> additionalProperties.put(k, new StringBuilder()));
        }

        if (expectedToolNames == null || expectedToolNames.isEmpty()) {
            this.expectedToolNames = Collections.emptySet();
        } else {
            this.expectedToolNames = new LinkedHashSet<>(expectedToolNames);
        }
    }

    public ChatCompletionAccumulator remapToolCallIdToUuid7() {
        this.remapToolCallIdToUuid7 = true;
        return this;
    }

    /**
     * 自定义 reasoning 字段名，按顺序优先匹配。
     * 默认: "reasoning", "thinking", "reasoning_content"
     */
    public ChatCompletionAccumulator reasoningKeys(String... keys) {
        if (keys != null && keys.length > 0) {
            this.reasoningKeys = List.of(keys);
        }
        return this;
    }

    // ==================== tool_call chunk 回调注册 API ====================

    /**
     * 每收到一个有效的 tool_call arguments/input chunk，触发一次。
     * <p>
     * 注意：id / type 为空时不会回调；functionName 不在 expectedToolNames 内时不会回调；
     * functionArguments 不是累计值。
     * <p>
     * 如果未设置 expectedToolNames（空集合），则只要 functionName 非空即回调（向后兼容）。
     */
    public ChatCompletionAccumulator onToolCallChunk(
            Consumer<ToolCallChunk> callback) {
        this.onToolCallChunk = callback;
        return this;
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
    public static class ToolCallChunk {
        private int index;
        private String id;
        private String originalId;
        private String type;
        private String functionName;
        private String functionArguments;

        public String getFunctionArguments() {
            return functionArguments == null ? "" : functionArguments;
        }

        @Override
        public String toString() {
            return "ToolCallChunk{" +
                    "index=" + index +
                    ", id='" + id + '\'' +
                    ", originalId='" + originalId + '\'' +
                    ", type='" + type + '\'' +
                    ", functionName='" + functionName + '\'' +
                    ", functionArguments='" + getFunctionArguments() + '\'' +
                    '}';
        }
    }

    @Getter
    public static class AccumulatedToolCall {
        private int index;
        private String id;
        private String originalId;
        private String type;
        private String functionName;
        private String aiId;
        /** 实际命中的 reasoning 字段名，如 "thinking"、"reasoning_content"，构建上下文时用同一个 key 回传 */
        private String reasoningKey;
        private final StringBuilder reasoning = new StringBuilder();
        private final StringBuilder functionArguments = new StringBuilder();

        public String getReasoning() {
            return reasoning.toString();
        }

        public String getFunctionArguments() {
            return functionArguments.toString();
        }

        @Override
        public String toString() {
            return "AccumulatedToolCall{" +
                    "index=" + index +
                    ", id='" + id + '\'' +
                    ", originalId='" + originalId + '\'' +
                    ", type='" + type + '\'' +
                    ", functionName='" + functionName + '\'' +
                    ", aiId='" + aiId + '\'' +
                    ", reasoningKey='" + reasoningKey + '\'' +
                    ", reasoning='" + reasoning + '\'' +
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
        @Getter
        private final String aiId;

        private AccumulatedResult(ChatCompletionAccumulator acc) {
            this.id = acc.id;
            this.model = acc.model;
            this.finishReason = acc.finishReason;
            this.content = acc.content.toString();
            this.refusal = acc.refusal.toString();
            this.toolCalls = Collections.unmodifiableList(new ArrayList<>(acc.toolCalls.values()));
            this.usage = acc.usage == null ? null : acc.usage.copy();
            this.aiId = acc.aiId;

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
                    ", aiId='" + aiId + '\'' +
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

        ToolCallChunk currentChunk = new ToolCallChunk();
        currentChunk.index = index;

        AccumulatedToolCall merged = toolCalls.computeIfAbsent(index, i -> {
            AccumulatedToolCall m = new AccumulatedToolCall();
            m.index = i;
            m.aiId = this.aiId;
            return m;
        });

        String rawId = tc.getString("id");
        if (!isBlank(rawId)) {
            String resolvedId = resolveId(rawId);
            if (merged.originalId == null) {
                merged.originalId = rawId;
                merged.id = resolvedId;
            }
        }

        String type = tc.getString("type");
        if (!isBlank(type) && merged.type == null) {
            merged.type = type;
        }

        JsonObject function = tc.getJsonObject("function");
        if (function != null && (merged.type == null || "function".equals(merged.type))) {
            if (merged.type == null) merged.type = "function";

            String name = function.getString("name");
            if (!isBlank(name) && merged.functionName == null) {
                merged.functionName = name;
            }

            String arguments = function.getString("arguments");
            if (!isEmpty(arguments)) {
                currentChunk.functionArguments = arguments;
                merged.functionArguments.append(arguments);
            }
        }

        JsonObject custom = tc.getJsonObject("custom");
        if (custom != null && (merged.type == null || "custom".equals(merged.type))) {
            if (merged.type == null) merged.type = "custom";

            String name = custom.getString("name");
            if (!isBlank(name) && merged.functionName == null) {
                merged.functionName = name;
            }

            String input = custom.getString("input");
            if (!isEmpty(input)) {
                currentChunk.functionArguments = input;
                merged.functionArguments.append(input);
            }
        }

        // ---------- 思考/推理字段 ----------
        String reasoningVal = null;
        String matchedKey = null;
        // 先从 tool_call 顶层找
        for (String key : reasoningKeys) {
            String val = tc.getString(key);
            if (!isEmpty(val)) {
                reasoningVal = val;
                matchedKey = key;
                break;
            }
        }
        // 有些模型嵌套在 function/custom 内部
        if (isEmpty(reasoningVal)) {
            JsonObject container = function != null ? function : custom;
            if (container != null) {
                for (String key : reasoningKeys) {
                    String val = container.getString(key);
                    if (!isEmpty(val)) {
                        reasoningVal = val;
                        matchedKey = key;
                        break;
                    }
                }
            }
        }
        if (!isEmpty(reasoningVal)) {
            if (merged.reasoningKey == null) {
                merged.reasoningKey = matchedKey;
            }
            merged.reasoning.append(reasoningVal);
        }

        // 元信息每次都返回已知的完整值；functionArguments 只返回当前 chunk 片段。
        currentChunk.id = merged.id;
        currentChunk.originalId = merged.originalId;
        currentChunk.type = merged.type;
        currentChunk.functionName = merged.functionName;

        emitToolCallChunk(currentChunk);
    }

    private String resolveId(String originalId) {
        if (!remapToolCallIdToUuid7) return originalId;
        return toolCallIdMapping.computeIfAbsent(
                originalId, k -> CommonUtils.uuid7().toString());
    }

    /**
     * 触发 tool_call chunk 回调。
     * <p>
     * 只有 id / type 都已经解析到，且 functionName 匹配 expectedToolNames 白名单
     * （白名单为空时退化为 functionName 非空即可），且当前 chunk 存在 arguments/input 片段时才回调。
     */
    private void emitToolCallChunk(ToolCallChunk chunk) {
        if (onToolCallChunk == null) return;
        if (chunk == null) return;
        if (isBlank(chunk.id) || isBlank(chunk.type)) return;
        if (isBlank(chunk.functionName)) return;
        if (!expectedToolNames.isEmpty() && !expectedToolNames.contains(chunk.functionName)) return;
        if (isEmpty(chunk.functionArguments)) return;

        try {
            onToolCallChunk.accept(chunk);
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