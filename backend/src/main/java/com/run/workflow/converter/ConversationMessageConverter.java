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
    // 扁平内容列表可能包含多种类型（如 [QUESTION, TOOL, TOOL]），
    // 需要按类型分组后分别生成对应的 OpenAI 消息。

    private static List<ChatCompletionMessageParam> convert(List<Object> contents, ContentConvertConfig config) {
        ContentConvertConfig convertConfig = config == null ? ContentConvertConfig.defaultConfig() : config;
        List<ChatCompletionMessageParam> result = new ArrayList<>();

        // 按消息类型分组
        List<JsonObject> questionItems = new ArrayList<>();
        List<JsonObject> systemItems = new ArrayList<>();
        List<JsonObject> textItems = new ArrayList<>();
        List<JsonObject> toolItems = new ArrayList<>();

        for (JsonObject obj : streamContent(contents).toList()) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) continue;
            switch (type) {
                case QUESTION -> questionItems.add(obj);
                case SYSTEM -> systemItems.add(obj);
                case TEXT -> textItems.add(obj);
                case TOOL -> toolItems.add(obj);
            }
        }

        // 1. system 消息
        if (!systemItems.isEmpty()) {
            String text = systemItems.stream()
                    .map(obj -> convertConfig.extract(ContentTypeConstants.SYSTEM, obj))
                    .filter(s -> !isBlank(s))
                    .collect(Collectors.joining());
            if (!isBlank(text)) {
                result.add(ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder().content(text).build()));
            }
        }

        // 2. user 消息
        if (!questionItems.isEmpty()) {
            String text = questionItems.stream()
                    .map(obj -> convertConfig.extract(ContentTypeConstants.QUESTION, obj))
                    .filter(s -> !isBlank(s))
                    .collect(Collectors.joining());
            result.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(text).build()));
        }

        // 3. assistant 消息（text + tool_calls）+ tool 结果消息
        if (!textItems.isEmpty() || !toolItems.isEmpty()) {
            String text = textItems.stream()
                    .map(obj -> convertConfig.extract(ContentTypeConstants.TEXT, obj))
                    .filter(s -> !isBlank(s))
                    .collect(Collectors.joining());

            JsonObject assistantMessage = new JsonObject().put("role", "assistant");
            if (!isBlank(text)) {
                assistantMessage.put("content", text);
            } else if (toolItems.isEmpty()) {
                assistantMessage.put("content", "");
            }

            if (!toolItems.isEmpty()) {
                JsonArray toolCalls = new JsonArray();
                for (JsonObject obj : toolItems) {
                    toolCalls.add(buildToolCall(obj));
                }
                assistantMessage.put("tool_calls", toolCalls);
            }

            result.add(ChatCompletionMessageParam.fromJsonObject(assistantMessage));

            // 每个 tool 结果生成一条 tool 消息
            for (JsonObject tc : toolItems) {
                if (!isBlank(tc.getString("result"))) {
                    result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(tc)));
                }
            }
        }

        return result;
    }

    private static JsonObject buildToolCall(JsonObject obj) {
        return new JsonObject()
                .put("id", obj.getString("id"))
                .put("type", "function")
                .put("function", new JsonObject()
                        .put("name", obj.getString("functionName"))
                        .put("arguments", obj.getString("arguments", "{}"))
                );
    }

    private static JsonObject buildToolMessage(JsonObject obj) {
        return new JsonObject()
                .put("role", "tool")
                .put("tool_call_id", obj.getString("id"))
                .put("content", obj.getString("result", ""));
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
