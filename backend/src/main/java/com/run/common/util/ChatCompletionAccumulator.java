package com.run.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.*;
import com.openai.models.completions.CompletionUsage;
import lombok.Getter;


import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  01:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class ChatCompletionAccumulator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String id;
    private String model;
    private String finishReason;
    private CompletionUsage usage;
    private final StringBuilder content = new StringBuilder();
    private final StringBuilder refusal = new StringBuilder();
    private final Map<Integer, AccumulatedToolCall> toolCalls = new TreeMap<>();

    // 需要收集的 additionalProperties key 列表
    private final Set<String> trackedKeys;
    // key -> StringBuilder 累加内容
    private final Map<String, StringBuilder> additionalProperties = new LinkedHashMap<>();

    // ====== 构造器 ======
    public ChatCompletionAccumulator() {
        this.trackedKeys = Collections.emptySet();
    }

    public ChatCompletionAccumulator(Collection<String> trackedKeys) {
        this.trackedKeys = new LinkedHashSet<>(trackedKeys);
        trackedKeys.forEach(k -> additionalProperties.put(k, new StringBuilder()));
    }

    // ====== ToolCall ======
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
                    ", functionName='" + functionName + '\'' +
                    ", functionArguments='" + functionArguments + '\'' +
                    '}';
        }
    }

    // ====== Result ======
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
         * 拒绝原因文本
         */
        @Getter
        private final String refusal;
        @Getter
        private final List<AccumulatedToolCall> toolCalls;

        private final CompletionUsage usage;

        @Getter
        private final boolean isToolCall;
        @Getter
        private final boolean isLegacyFunctionCall;
        @Getter
        private final boolean isTextResponse;
        @Getter
        private final boolean isAborted;
        /**
         * 模型拒绝回答
         */
        private final boolean isRefusal;

        public boolean isRefusal() {
            return isRefusal;
        }

        /**
         * 流结束但没有收到 finish_reason，说明异常断流
         */
        @Getter
        private final boolean isIncomplete;
        /**
         * arguments 是否合法 JSON
         */
        @Getter
        private final Map<String, Boolean> toolCallArgumentsValid;

        // additionalProperties 收集结果 key -> 完整内容
        @Getter
        private final Map<String, String> additionalProperties;

        private AccumulatedResult(ChatCompletionAccumulator acc) {
            this.id = acc.id;
            this.model = acc.model;
            this.finishReason = acc.finishReason;
            this.content = acc.content.toString();
            this.refusal = acc.refusal.toString();
            this.toolCalls = Collections.unmodifiableList(new ArrayList<>(acc.toolCalls.values()));
            this.usage = acc.usage;

            this.isToolCall = "tool_calls".equals(finishReason) || !this.toolCalls.isEmpty();
            this.isLegacyFunctionCall = "function_call".equals(finishReason);
            this.isTextResponse = "stop".equals(finishReason);
            this.isAborted = "length".equals(finishReason) || "content_filter".equals(finishReason);
            this.isRefusal = !this.refusal.isEmpty();
            this.isIncomplete = finishReason == null;

            Map<String, Boolean> validMap = new LinkedHashMap<>();
            for (AccumulatedToolCall tc : this.toolCalls) {
                validMap.put(tc.getFunctionName(), isValidJson(tc.getFunctionArguments()));
            }
            this.toolCallArgumentsValid = Collections.unmodifiableMap(validMap);

            // 快照 additionalProperties
            Map<String, String> snapshot = new LinkedHashMap<>();
            acc.additionalProperties.forEach((k, v) -> snapshot.put(k, v.toString()));
            this.additionalProperties = Collections.unmodifiableMap(snapshot);
        }

        // 获取指定 key 的内容
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

        public Optional<CompletionUsage> getUsage() {
            return Optional.ofNullable(usage);
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
        if (completion == null) return this;

        if (this.id == null) this.id = completion.id();
        if (this.model == null) this.model = completion.model();

        completion.usage().ifPresent(u -> this.usage = u);

        for (ChatCompletion.Choice choice : completion.choices()) {

            // ===== finishReason =====
            ChatCompletion.Choice.FinishReason fr = choice.finishReason();
            this.finishReason = fr.asString();

            ChatCompletionMessage message = choice.message();

            // ===== content =====
            message.content().ifPresent(content::append);

            // ===== refusal =====
            message.refusal().ifPresent(refusal::append);

            // ===== tool_calls =====
            message.toolCalls().ifPresent(tcs -> {
                for (int i = 0; i < tcs.size(); i++) {

                    ChatCompletionMessageToolCall tc = tcs.get(i);

                    AccumulatedToolCall merged = toolCalls.computeIfAbsent(i, idx -> {
                        AccumulatedToolCall m = new AccumulatedToolCall();
                        m.index = idx;
                        return m;
                    });

                    // ===== function =====
                    if (tc.isFunction()) {

                        ChatCompletionMessageFunctionToolCall func = tc.function().orElse(null);
                        if (func == null) continue;

                        if (merged.id == null) merged.id = func.id();
                        if (merged.type == null) merged.type = "function";

                        if (merged.functionName == null) {
                            merged.functionName = func.function().name();
                        }

                        merged.functionArguments.append(func.function().arguments());
                    }

                    // ===== custom =====
                    else if (tc.isCustom()) {

                        ChatCompletionMessageCustomToolCall customTc = tc.custom().orElse(null);
                        if (customTc == null) continue;

                        if (merged.id == null) merged.id = customTc.id();
                        if (merged.type == null) merged.type = "custom";

                        ChatCompletionMessageCustomToolCall.Custom custom = customTc.custom();

                        // ✅ name
                        if (merged.functionName == null) {
                            merged.functionName = custom.name();
                        }

                        // ✅ input（注意：已经是 String）
                        merged.functionArguments.append(custom.input());
                    }

                    // ===== fallback（极端情况）=====
                    else {
                        tc._json().ifPresent(merged.functionArguments::append);
                    }
                }
            });

            // ===== additionalProperties（Choice级别）=====
            if (!trackedKeys.isEmpty()) {
                choice._additionalProperties().forEach((k, v) -> {
                    StringBuilder sb = additionalProperties.get(k);
                    if (sb != null && v != null && !v.isNull()) {
                        sb.append(v.toString());
                    }
                });
            }

            // ===== additionalProperties（Message级别）=====
            if (!trackedKeys.isEmpty()) {
                message._additionalProperties().forEach((k, v) -> {
                    StringBuilder sb = additionalProperties.get(k);
                    if (sb != null && v != null && !v.isNull()) {
                        sb.append(v.toString());
                    }
                });
            }
        }

        // ===== completion级 additionalProperties =====
        if (!trackedKeys.isEmpty()) {
            completion._additionalProperties().forEach((k, v) -> {
                StringBuilder sb = additionalProperties.get(k);
                if (sb != null && v != null && !v.isNull()) {
                    sb.append(v);
                }
            });
        }

        return this;
    }

    // ====== append ======
    public ChatCompletionAccumulator append(ChatCompletionChunk chunk) {
        if (this.id == null) this.id = chunk.id();
        if (this.model == null) this.model = chunk.model();

        chunk.usage().ifPresent(u -> this.usage = u);

        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            choice.finishReason().ifPresent(fr -> this.finishReason = fr.toString());

            ChatCompletionChunk.Choice.Delta delta = choice.delta();
            delta.content().ifPresent(content::append);
            delta.refusal().ifPresent(refusal::append);
            delta.toolCalls().ifPresent(tcs -> tcs.forEach(this::appendToolCall));

            // ✅ 收集指定的 additionalProperties
            if (!trackedKeys.isEmpty()) {
                delta._additionalProperties().forEach((k, v) -> {
                    StringBuilder sb = additionalProperties.get(k);
                    if (sb != null && !v.isNull()) {
                        // JsonValue 可能是字符串或对象，统一转为字符串追加
                        String text = v.toString();
                        sb.append(text);
                    }
                });
            }
        }
        return this;
    }

    private void appendToolCall(ChatCompletionChunk.Choice.Delta.ToolCall tc) {
        AccumulatedToolCall merged = toolCalls.computeIfAbsent((int) tc.index(), i -> {
            AccumulatedToolCall m = new AccumulatedToolCall();
            m.index = i;
            return m;
        });
        tc.id().ifPresent(v -> {
            if (merged.id == null) merged.id = v;
        });
        tc.type().ifPresent(v -> {
            if (merged.type == null) merged.type = v.toString();
        });
        tc.function().ifPresent(func -> {
            func.name().ifPresent(v -> {
                if (merged.functionName == null) merged.functionName = v;
            });
            func.arguments().ifPresent(merged.functionArguments::append);
        });
    }

    // ====== complete ======
    public AccumulatedResult complete() {
        return new AccumulatedResult(this);
    }
}