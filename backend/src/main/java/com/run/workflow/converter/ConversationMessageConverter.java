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
    //   - TOOL            -> 不合并；每个 ToolCallContent 都单独生成：
    //                       assistant.tool_calls -> tool
    //
    // 这样连续 TOOL 会被表示成多轮工具调用：
    //   assistant.tool_calls(read_file)
    //   tool(read_file result)
    //   assistant.tool_calls(apply_patch)
    //   tool(apply_patch result)
    //
    // 这样能保留工具调用之间的前后依赖关系，避免把它们误合并成同一轮并发 tool_calls。
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

        for (JsonObject obj : streamContent(contents).toList()) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) {
                continue;
            }
            String extracted = cfg.extract(type, obj);

            switch (type) {
                case SYSTEM -> {
                    flushUser(userBuf, result);
                    flushAssistantText(assistantTextBuf, result);
                    if (!isBlank(extracted)) {
                        systemBuf.append(extracted);
                    }
                }
                case QUESTION -> {
                    flushSystem(systemBuf, result);
                    flushAssistantText(assistantTextBuf, result);
                    if (!isBlank(extracted)) {
                        userBuf.append(extracted);
                    }
                }
                case TEXT -> {
                    flushSystem(systemBuf, result);
                    flushUser(userBuf, result);
                    if (!isBlank(extracted)) {
                        assistantTextBuf.append(extracted);
                    }
                }
                case TOOL -> {
                    flushSystem(systemBuf, result);
                    flushUser(userBuf, result);

                    // TOOL 不进入 pending 列表，也不和相邻 TOOL 合并。
                    // 如果前面已经有 assistant 文本，先输出 assistant.content；
                    // 当前 TOOL 再单独输出一轮 assistant.tool_calls -> tool。
                    flushAssistantText(assistantTextBuf, result);
                    appendToolTurn(obj, result);
                }
            }
        }

        // 收尾
        flushSystem(systemBuf, result);
        flushUser(userBuf, result);
        flushAssistantText(assistantTextBuf, result);

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

    private static void flushAssistantText(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) {
            return;
        }

        JsonObject assistantMsg = new JsonObject()
                .put("role", "assistant")
                .put("content", buf.toString());

        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMsg));
        buf.setLength(0);
    }

    private static void appendToolTurn(JsonObject obj, List<ChatCompletionMessageParam> result) {
        JsonObject assistantMsg = new JsonObject()
                .put("role", "assistant")
                .put("tool_calls", new JsonArray().add(buildToolCall(obj)));

        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMsg));
        result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(obj)));
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
