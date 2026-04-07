package com.run.workflow.converter;

import com.openai.models.chat.completions.*;
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
 * {@code @注释: }
 */
public class ConversationMessageConverter {

    private ConversationMessageConverter() {
    }

    // ── 公开接口 ──────────────────────────────────────────────────────────────

    public static List<ChatCompletionMessageParam> toOpenAiMessages(List<ConversationMessage> messages) {
        return toOpenAiMessages(messages, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessages(List<ConversationMessage> messages, ContentConvertConfig config) {
        List<ChatCompletionMessageParam> result = new ArrayList<>();
        for (ConversationMessage message : messages) {
            result.addAll(toOpenAiMessage(message.getContent(), config));
        }
        return result;
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message) {
        return toOpenAiMessage(message.getContent(), ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message, ContentConvertConfig config) {
        return toOpenAiMessage(message.getContent(), config);
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents) {
        return toOpenAiMessage(contents, ContentConvertConfig.builder().build());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents, ContentConvertConfig config) {
        if (streamContent(contents).anyMatch(obj -> ContentTypeConstants.QUESTION == parseType(obj))) {
            return List.of(buildUserMessage(contents, config));
        }
        if (streamContent(contents).anyMatch(obj -> ContentTypeConstants.SYSTEM == parseType(obj))) {
            return List.of(buildSystemMessage(contents, config));
        }
        return buildAssistantMessages(contents, config);
    }

    // ── USER ──────────────────────────────────────────────────────────────────

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

    // ── SYSTEM ────────────────────────────────────────────────────────────────

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

    // ── ASSISTANT ─────────────────────────────────────────────────────────────

    private static List<ChatCompletionMessageParam> buildAssistantMessages(JsonArray contents, ContentConvertConfig config) {
        List<JsonObject> toolContents = streamContent(contents)
                .filter(obj -> ContentTypeConstants.TOOL == parseType(obj))
                .filter(obj -> !isBlank(obj.getString("result")))
                .collect(Collectors.toList());

        List<ChatCompletionMessageParam> result = new ArrayList<>();

        ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                ChatCompletionAssistantMessageParam.builder();

        String text = extractText(contents, config);
        if (!isBlank(text)) {
            assistantBuilder.content(text);
        }

        if (!toolContents.isEmpty()) {
            List<ChatCompletionMessageToolCall> toolCalls = toolContents.stream()
                    .map(obj -> ChatCompletionMessageToolCall.ofFunction(
                            ChatCompletionMessageFunctionToolCall.builder()
                                    .id(obj.getString("id"))
                                    .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                                            .name(obj.getString("functionName"))
                                            .arguments(obj.getString("arguments", "{}"))
                                            .build())
                                    .build()
                    ))
                    .collect(Collectors.toList());
            assistantBuilder.toolCalls(toolCalls);
        }

        result.add(ChatCompletionMessageParam.ofAssistant(assistantBuilder.build()));

        for (JsonObject tc : toolContents) {
            result.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(tc.getString("id"))
                            .content(tc.getString("result"))
                            .build()
            ));
        }

        return result;
    }

    // ── 文本提取（按 config 策略） ─────────────────────────────────────────────

    private static String extractText(JsonArray contents, ContentConvertConfig config) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        streamContent(contents).forEach(obj -> {
            ContentTypeConstants type = parseType(obj);
            if (type == null) return;

            ContentConvertStrategy strategy = config.getStrategy(type);
            switch (strategy) {
                case IGNORE -> {
                }
                case TEXT -> {
                    String text = config.extract(type, obj);
                    if (!isBlank(text)) sb.append(text);
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

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static Stream<JsonObject> streamContent(JsonArray contents) {
        if (contents == null) return Stream.empty();
        return IntStream.range(0, contents.size())
                .mapToObj(contents::getJsonObject)
                .filter(Objects::nonNull);
    }

    private static ContentTypeConstants parseType(JsonObject obj) {
        String type = obj.getString("type");
        if (isBlank(type)) return null;
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