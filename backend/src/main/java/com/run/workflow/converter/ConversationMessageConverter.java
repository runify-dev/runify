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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents) {
        return toOpenAiMessage(contents, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents, ContentConvertConfig config) {
        ContentConvertConfig convertConfig = config == null ? ContentConvertConfig.defaultConfig() : config;

        if (streamContent(contents).anyMatch(obj -> ContentTypeConstants.QUESTION == parseType(obj))) {
            return List.of(buildUserMessage(contents, convertConfig));
        }
        if (streamContent(contents).anyMatch(obj -> ContentTypeConstants.SYSTEM == parseType(obj))) {
            return List.of(buildSystemMessage(contents, convertConfig));
        }
        return buildAssistantMessages(contents, convertConfig);
    }

    private static ChatCompletionMessageParam buildUserMessage(JsonArray contents, ContentConvertConfig config) {
        String text = streamContent(contents)
                .filter(obj -> ContentTypeConstants.QUESTION == parseType(obj))
                .map(obj -> config.extract(ContentTypeConstants.QUESTION, obj))
                .filter(s -> !isBlank(s))
                .collect(Collectors.joining());

        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(text)
                        .build()
        );
    }

    private static ChatCompletionMessageParam buildSystemMessage(JsonArray contents, ContentConvertConfig config) {
        String text = streamContent(contents)
                .filter(obj -> ContentTypeConstants.SYSTEM == parseType(obj))
                .map(obj -> config.extract(ContentTypeConstants.SYSTEM, obj))
                .filter(s -> !isBlank(s))
                .collect(Collectors.joining());

        return ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(text)
                        .build()
        );
    }

    private static List<ChatCompletionMessageParam> buildAssistantMessages(JsonArray contents,
                                                                           ContentConvertConfig config) {
        List<JsonObject> toolContents = streamContent(contents)
                .filter(obj -> ContentTypeConstants.TOOL == parseType(obj))
                .filter(obj -> !isBlank(obj.getString("result")))
                .collect(Collectors.toList());

        List<ChatCompletionMessageParam> result = new ArrayList<>();

        String text = extractText(contents, config);

        JsonObject assistantMessage = new JsonObject()
                .put("role", "assistant");

        if (!isBlank(text)) {
            assistantMessage.put("content", text);
        } else if (toolContents.isEmpty()) {
            assistantMessage.put("content", "");
        }

        if (!toolContents.isEmpty()) {
            JsonArray toolCalls = new JsonArray();
            for (JsonObject obj : toolContents) {
                toolCalls.add(buildToolCall(obj));
            }
            assistantMessage.put("tool_calls", toolCalls);
        }

        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMessage));

        for (JsonObject tc : toolContents) {
            result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(tc)));
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

    private static String extractText(JsonArray contents, ContentConvertConfig config) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        streamContent(contents).forEach(obj -> {
            ContentTypeConstants type = parseType(obj);
            if (type == null) {
                return;
            }

            ContentConvertStrategy strategy = config.getStrategy(type);
            switch (strategy) {
                case IGNORE -> {
                }
                case TEXT -> {
                    String text = config.extract(type, obj);
                    if (!isBlank(text)) {
                        sb.append(text);
                    }
                }
                case PREFIXED -> {
                    String text = config.extract(type, obj);
                    if (!isBlank(text)) {
                        sb.append(config.getPrefix(type))
                                .append(text)
                                .append("\n");
                    }
                }
            }
        });
        return sb.toString().trim();
    }

    private static Stream<JsonObject> streamContent(JsonArray contents) {
        if (contents == null) {
            return Stream.empty();
        }
        return IntStream.range(0, contents.size())
                .mapToObj(contents::getJsonObject)
                .filter(Objects::nonNull);
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