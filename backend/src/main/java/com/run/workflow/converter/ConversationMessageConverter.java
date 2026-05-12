package com.run.workflow.converter;

import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.constants.ContentTypeConstants;
import com.run.dao.entity.ConversationMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/6  17:40}
 * {@code @Version 1.0}
 * {@code @注释: 适配自用轻量 openai chat sdk}
 */
public class ConversationMessageConverter {

    private ConversationMessageConverter() {
    }

    // ── ConversationMessage 入口 ──

    public static List<ChatCompletionMessageParam> toOpenAiMessages(List<ConversationMessage> messages) {
        return toOpenAiMessages(messages, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessages(List<ConversationMessage> messages,
                                                                    ContentConvertConfig config) {
        List<ChatCompletionMessageParam> result = new ArrayList<>();
        if (messages == null || messages.isEmpty()) {
            return result;
        }
        for (ConversationMessage message : messages) {
            if (message == null) {
                continue;
            }
            result.addAll(toOpenAiMessage(message.getContent(), config));
        }
        return result;
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message) {
        if (message == null) {
            return List.of();
        }
        return toOpenAiMessage(message.getContent(), ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message,
                                                                   ContentConvertConfig config) {
        if (message == null) {
            return List.of();
        }
        return toOpenAiMessage(message.getContent(), config);
    }

    // ── JsonArray 入口（兼容旧代码）──

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents) {
        return toOpenAiMessage(contents, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents, ContentConvertConfig config) {
        return convert(toList(contents), config);
    }

    // ── List<Object> 入口（新格式，ChatStartNode 存的是 List）──

    public static List<ChatCompletionMessageParam> toOpenAiMessage(List<Object> contents) {
        return toOpenAiMessage(contents, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(List<Object> contents, ContentConvertConfig config) {
        return convert(contents, config);
    }

    // ── 核心转换逻辑 ──
    //
    // 严格保留原始顺序，同类型相邻自动合并：
    //   - 连续 SYSTEM     -> 合并为一条 system
    //   - 连续 QUESTION   -> 合并为一条 user
    //   - 连续 TEXT       -> 合并为一条 assistant.content
    //   - 连续 TOOL       -> 一条 ToolCallContent 拆成 assistant.tool_calls 项 + tool 消息；
    //                       多条相邻 TOOL 共用同一条 assistant，tool 消息按顺序紧跟
    //
    // 严格符合 OpenAI 协议：
    //   - 每个 tool 消息紧跟在声明对应 tool_call_id 的 assistant 之后
    //   - tool_calls 之后再出现 TEXT，会开启新的一轮 assistant
    //   - 允许 user 之后直接是 tool_calls（assistant 可以只有 tool_calls 没有 content）

    private static List<ChatCompletionMessageParam> convert(List<Object> contents, ContentConvertConfig config) {
        ContentConvertConfig cfg = config == null ? ContentConvertConfig.defaultConfig() : config;
        List<ChatCompletionMessageParam> result = new ArrayList<>();

        StringBuilder systemBuf = new StringBuilder();
        StringBuilder userBuf = new StringBuilder();
        StringBuilder assistantTextBuf = new StringBuilder();
        List<JsonObject> pendingToolCalls = new ArrayList<>();

        for (JsonObject obj : streamContent(contents).toList()) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) {
                continue;
            }
            String extracted = cfg.extract(type, obj);

            switch (type) {
                case SYSTEM -> {
                    flushUser(userBuf, result);
                    flushAssistantAndTools(assistantTextBuf, pendingToolCalls, result);
                    if (!isBlank(extracted)) {
                        systemBuf.append(extracted);
                    }
                }
                case QUESTION -> {
                    flushSystem(systemBuf, result);
                    flushAssistantAndTools(assistantTextBuf, pendingToolCalls, result);
                    if (!isBlank(extracted)) {
                        userBuf.append(extracted);
                    }
                }
                case TEXT -> {
                    flushSystem(systemBuf, result);
                    flushUser(userBuf, result);
                    // 已有 pendingToolCalls 意味着上一轮 assistant 已封口，
                    // 当前 TEXT 是 tool 结果之后新一轮 assistant 发言
                    if (!pendingToolCalls.isEmpty()) {
                        flushAssistantAndTools(assistantTextBuf, pendingToolCalls, result);
                    }
                    if (!isBlank(extracted)) {
                        assistantTextBuf.append(extracted);
                    }
                }
                case TOOL -> {
                    flushSystem(systemBuf, result);
                    flushUser(userBuf, result);
                    pendingToolCalls.add(obj);
                }
            }
        }

        // 收尾
        flushSystem(systemBuf, result);
        flushUser(userBuf, result);
        flushAssistantAndTools(assistantTextBuf, pendingToolCalls, result);

        return result;
    }

    // ── flush 辅助方法 ──

    private static void flushSystem(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) {
            return;
        }
        result.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(buf.toString()).build()));
        buf.setLength(0);
    }

    private static void flushUser(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) {
            return;
        }
        result.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(buf.toString()).build()));
        buf.setLength(0);
    }

    private static void flushAssistantAndTools(StringBuilder textBuf,
                                               List<JsonObject> pendingToolCalls,
                                               List<ChatCompletionMessageParam> result) {
        if (textBuf.length() == 0 && pendingToolCalls.isEmpty()) {
            return;
        }

        JsonObject assistantMsg = new JsonObject().put("role", "assistant");

        if (textBuf.length() > 0) {
            assistantMsg.put("content", textBuf.toString());
        }

        if (!pendingToolCalls.isEmpty()) {
            // 多条相邻 TOOL 全部合并到这一条 assistant 的 tool_calls 数组
            JsonArray toolCallsArr = new JsonArray();
            for (JsonObject tc : pendingToolCalls) {
                toolCallsArr.add(buildToolCall(tc));
            }
            assistantMsg.put("tool_calls", toolCallsArr);
        } else if (textBuf.length() == 0) {
            // 兜底：没有 tool_calls 也没有 content 时给空串，避免 content=null
            assistantMsg.put("content", "");
        }

        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMsg));

        // 紧跟着按顺序输出每条 tool 消息，与 tool_calls 一一对应
        for (JsonObject tc : pendingToolCalls) {
            result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(tc)));
        }

        textBuf.setLength(0);
        pendingToolCalls.clear();
    }

    // ── ToolCallContent -> OpenAI tool_call / tool message ──
    //
    // ToolCallContent 字段映射：
    //   toolName            -> function.name
    //   functionArguments   -> function.arguments
    //   content             -> tool message 的 content（即工具执行结果）
    //   id                  -> tool_call_id

    private static JsonObject buildToolCall(JsonObject obj) {
        String id = obj.getString("id");
        String name = obj.getString("toolName");
        String arguments = obj.getString("functionArguments");
        if (isBlank(arguments)) {
            arguments = "{}";
        }
        return new JsonObject()
                .put("id", id == null ? "" : id)
                .put("type", "function")
                .put("function", new JsonObject()
                        .put("name", name == null ? "" : name)
                        .put("arguments", arguments)
                );
    }

    private static JsonObject buildToolMessage(JsonObject obj) {
        String id = obj.getString("id");
        Object rawContent = obj.getValue("content");
        String content;
        if (rawContent == null) {
            content = "";
        } else if (rawContent instanceof String s) {
            content = s;
        } else {
            content = rawContent.toString();
        }
        return new JsonObject()
                .put("role", "tool")
                .put("tool_call_id", id == null ? "" : id)
                .put("content", content);
    }

    // ── 工具方法 ──

    private static Stream<JsonObject> streamContent(List<Object> contents) {
        if (contents == null) {
            return Stream.empty();
        }
        return contents.stream()
                .filter(Objects::nonNull)
                .map(ConversationMessageConverter::toJsonObject)
                .filter(Objects::nonNull);
    }

    @SuppressWarnings("unchecked")
    private static JsonObject toJsonObject(Object obj) {
        if (obj instanceof JsonObject jo) {
            return jo;
        }
        if (obj instanceof Map<?, ?> map) {
            return new JsonObject((Map<String, Object>) map);
        }
        return null;
    }

    private static List<Object> toList(JsonArray jsonArray) {
        if (jsonArray == null) {
            return List.of();
        }
        return jsonArray.getList();
    }

    private static ContentTypeConstants parseType(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String type = obj.getString("type");
        if (isBlank(type)) {
            return null;
        }
        try {
            return ContentTypeConstants.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}