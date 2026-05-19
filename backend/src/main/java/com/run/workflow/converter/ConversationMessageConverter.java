package com.run.workflow.converter;

import com.run.ai.openai.chat.ChatCompletionMessageParam;
import com.run.ai.openai.chat.ChatCompletionSystemMessageParam;
import com.run.ai.openai.chat.ChatCompletionUserMessageParam;
import com.run.common.constants.ContentTypeConstants;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.MimeMapping;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
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

    // ══════════════════════════════════════════════
    //  公开入口（同步）
    // ══════════════════════════════════════════════

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
            if (message == null) continue;
            result.addAll(toOpenAiMessage(message.getContent(), config));
        }
        return result;
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message) {
        return message == null ? List.of() : toOpenAiMessage(message.getContent(), ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(ConversationMessage message,
                                                                   ContentConvertConfig config) {
        return message == null ? List.of() : toOpenAiMessage(message.getContent(), config);
    }

    // JsonArray 入口（兼容旧代码）
    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents) {
        return toOpenAiMessage(contents, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessage(JsonArray contents, ContentConvertConfig config) {
        return convert(toList(contents), config);
    }

    // List<Object> 入口
    public static List<ChatCompletionMessageParam> toOpenAiMessageFromList(List<Object> contents) {
        return toOpenAiMessageFromList(contents, ContentConvertConfig.defaultConfig());
    }

    public static List<ChatCompletionMessageParam> toOpenAiMessageFromList(List<Object> contents,
                                                                           ContentConvertConfig config) {
        return convert(contents, config);
    }

    // ══════════════════════════════════════════════
    //  公开入口（异步，支持 QUESTION 多模态）
    // ══════════════════════════════════════════════

    public static Future<List<ChatCompletionMessageParam>> toOpenAiMessageAsync(List<Object> contents,
                                                                                FileMapper fileMapper,
                                                                                Vertx vertx) {
        return convertAsync(contents, ContentConvertConfig.defaultConfig(), fileMapper, vertx);
    }

    // ══════════════════════════════════════════════
    //  同步核心转换（不处理文件）
    // ══════════════════════════════════════════════

    private static List<ChatCompletionMessageParam> convert(List<Object> contents, ContentConvertConfig config) {
        ContentConvertConfig cfg = config == null ? ContentConvertConfig.defaultConfig() : config;
        List<ChatCompletionMessageParam> result = new ArrayList<>();

        // 用显式状态机替代手动 flush 排列组合
        ContentTypeConstants currentRole = null;
        StringBuilder buf = new StringBuilder();

        for (JsonObject obj : toJsonObjectList(contents)) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) continue;

            String extracted = cfg.extract(type, obj);

            // TOOL 比较特殊，总是独立成一组，先处理
            if (type == ContentTypeConstants.TOOL) {
                flushBuffer(currentRole, buf, result);
                currentRole = null;
                appendToolTurn(obj, result);
                continue;
            }

            // 角色切换时 flush 前一个 buffer
            if (type != currentRole) {
                flushBuffer(currentRole, buf, result);
                currentRole = type;
            }

            if (!isBlank(extracted)) {
                buf.append(extracted);
            }
        }

        flushBuffer(currentRole, buf, result);
        return result;
    }

    /**
     * 根据当前角色类型，将 buffer 内容 flush 为对应的消息。
     */
    private static void flushBuffer(ContentTypeConstants role, StringBuilder buf,
                                    List<ChatCompletionMessageParam> result) {
        if (role == null || buf.length() == 0) return;
        switch (role) {
            case SYSTEM -> flushSystem(buf, result);
            case QUESTION -> flushUser(buf, result);
            case TEXT -> flushAssistantText(buf, result);
            default -> buf.setLength(0); // 不应该走到这里
        }
    }

    // ══════════════════════════════════════════════
    //  异步核心转换（支持 QUESTION 多模态）
    //
    //  修复：所有状态变更都串入 fileChain，
    //  保证在同一条异步链上顺序执行，消除并发隐患。
    // ══════════════════════════════════════════════

    private static Future<List<ChatCompletionMessageParam>> convertAsync(List<Object> contents,
                                                                         ContentConvertConfig config,
                                                                         FileMapper fileMapper,
                                                                         Vertx vertx) {
        ContentConvertConfig cfg = config == null ? ContentConvertConfig.defaultConfig() : config;

        // 所有可变状态
        List<ChatCompletionMessageParam> result = new ArrayList<>();
        StringBuilder systemBuf = new StringBuilder();
        StringBuilder userTextBuf = new StringBuilder();
        List<JsonObject> userFileParts = new ArrayList<>();
        StringBuilder assistantTextBuf = new StringBuilder();

        // 把全部操作串进一条异步链，保证顺序和线程安全
        Future<Void> chain = Future.succeededFuture();

        for (JsonObject obj : toJsonObjectList(contents)) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) continue;

            String extracted = cfg.extract(type, obj);

            switch (type) {
                case SYSTEM -> {
                    chain = chain.compose(v -> {
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        flushAssistantText(assistantTextBuf, result);
                        if (!isBlank(extracted)) {
                            systemBuf.append(extracted);
                        }
                        return Future.succeededFuture();
                    });
                }
                case QUESTION -> {
                    // 文件读取需要异步，所以 compose 进链
                    boolean needReadFiles = hasFiles(obj);
                    chain = chain.compose(v -> {
                        flushSystem(systemBuf, result);
                        flushAssistantText(assistantTextBuf, result);
                        if (!isBlank(extracted)) {
                            userTextBuf.append(extracted);
                        }
                        if (needReadFiles) {
                            return readQuestionFileParts(obj, fileMapper, vertx)
                                    .map(parts -> {
                                        userFileParts.addAll(parts);
                                        return null;
                                    });
                        }
                        return Future.succeededFuture();
                    });
                }
                case TEXT -> {
                    chain = chain.compose(v -> {
                        flushSystem(systemBuf, result);
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        if (!isBlank(extracted)) {
                            assistantTextBuf.append(extracted);
                        }
                        return Future.succeededFuture();
                    });
                }
                case TOOL -> {
                    chain = chain.compose(v -> {
                        flushSystem(systemBuf, result);
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        flushAssistantText(assistantTextBuf, result);
                        appendToolTurn(obj, result);
                        return Future.succeededFuture();
                    });
                }
            }
        }

        // 收尾
        return chain.compose(v -> {
            flushSystem(systemBuf, result);
            flushUserFromParts(userTextBuf, userFileParts, result);
            flushAssistantText(assistantTextBuf, result);
            return Future.succeededFuture(result);
        });
    }

    // ══════════════════════════════════════════════
    //  flush 辅助方法
    // ══════════════════════════════════════════════

    private static void flushSystem(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) return;
        result.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(buf.toString()).build()));
        buf.setLength(0);
    }

    private static void flushUser(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) return;
        result.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(buf.toString()).build()));
        buf.setLength(0);
    }

    private static void flushAssistantText(StringBuilder buf, List<ChatCompletionMessageParam> result) {
        if (buf.length() == 0) return;
        result.add(ChatCompletionMessageParam.fromJsonObject(
                new JsonObject().put("role", "assistant").put("content", buf.toString())));
        buf.setLength(0);
    }

    /**
     * 从 textBuf + fileParts 构造 user 消息并 flush。
     * 只在 fileChain 完成后调用，保证 fileParts 已填充。
     */
    private static void flushUserFromParts(StringBuilder textBuf,
                                           List<JsonObject> fileParts,
                                           List<ChatCompletionMessageParam> result) {
        if (textBuf.length() == 0 && fileParts.isEmpty()) return;

        if (fileParts.isEmpty()) {
            flushUser(textBuf, result);
            return;
        }

        // 多模态：text + image_url / video_url 等
        List<Object> contentParts = new ArrayList<>();
        if (textBuf.length() > 0) {
            contentParts.add(new JsonObject().put("type", "text").put("text", textBuf.toString()));
        }
        contentParts.addAll(fileParts);
        result.add(ChatCompletionMessageParam.fromJsonObject(
                new JsonObject().put("role", "user").put("content", new JsonArray(contentParts))));

        textBuf.setLength(0);
        fileParts.clear();
    }

    // ══════════════════════════════════════════════
    //  QUESTION 文件读取
    // ══════════════════════════════════════════════

    private static Future<List<JsonObject>> readQuestionFileParts(JsonObject obj,
                                                                  FileMapper fileMapper,
                                                                  Vertx vertx) {
        List<Future<JsonObject>> futures = new ArrayList<>();

        JsonArray images = safeGetJsonArray(obj, "images");
        if (images != null) {
            for (Object item : images) {
                JsonObject img = toJsonObject(item);
                if (img != null) {
                    futures.add(readFileAsContentPart(img, "image_url", "image", fileMapper, vertx));
                }
            }
        }

        JsonArray videos = safeGetJsonArray(obj, "videos");
        if (videos != null) {
            for (Object item : videos) {
                JsonObject vid = toJsonObject(item);
                if (vid != null) {
                    futures.add(readFileAsContentPart(vid, "video_url", "video", fileMapper, vertx));
                }
            }
        }

        JsonArray files = safeGetJsonArray(obj, "files");
        if (files != null && !files.isEmpty()) {
            String fileNames = files.stream()
                    .map(ConversationMessageConverter::toJsonObject)
                    .filter(Objects::nonNull)
                    .map(f -> f.getString("name", "unknown"))
                    .collect(Collectors.joining(", "));
            if (!isBlank(fileNames)) {
                futures.add(Future.succeededFuture(
                        new JsonObject().put("type", "text").put("text", "Files: " + fileNames)));
            }
        }

        if (futures.isEmpty()) {
            return Future.succeededFuture(List.of());
        }

        return Future.all(futures).map(composite -> {
            List<JsonObject> parts = new ArrayList<>();
            for (Future<JsonObject> f : futures) {
                JsonObject r = f.result();
                if (r != null) parts.add(r);
            }
            return parts;
        });
    }

    private static Future<JsonObject> readFileAsContentPart(JsonObject fileObj,
                                                            String urlType,
                                                            String mimeTypePrefix,
                                                            FileMapper fileMapper,
                                                            Vertx vertx) {
        String url = fileObj.getString("url", "");
        String fileId = extractFileId(url);
        if (isBlank(fileId)) {
            return Future.succeededFuture();
        }
        return fileMapper.getById(fileId)
                .compose(entity -> readBase64(fileMapper, entity, vertx)
                        .map(base64 -> {
                            String fileName = entity.getFileName();
                            String mime = MimeMapping.mimeTypeForFilename(fileName);
                            if (mime == null || !mime.startsWith(mimeTypePrefix)) {
                                mime = mimeTypePrefix + "/" + getExtension(fileName);
                            }
                            String dataUrl = "data:" + mime + ";base64," + base64;
                            return new JsonObject()
                                    .put("type", urlType)
                                    .put(urlType, new JsonObject().put("url", dataUrl));
                        }));
    }

    /**
     * 将文件流读取为 Base64 字符串。
     * 修复：
     *  1. handler 异常后立即 pause 流并短路，防止后续回调重复操作 promise。
     *  2. 用 failed 标志位确保 promise 只被 complete/fail 一次。
     */
    private static Future<String> readBase64(FileMapper fileMapper, FileEntity entity, Vertx vertx) {
        Promise<String> promise = Promise.promise();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        OutputStream b64Out = Base64.getEncoder().wrap(byteOut);
        boolean[] failed = {false}; // 用数组绕过 lambda 的 effectively final 限制

        var stream = fileMapper.downloadFile(vertx, entity);
        stream.handler(chunk -> {
                    if (failed[0]) return;
                    try {
                        b64Out.write(chunk.getBytes());
                    } catch (Exception e) {
                        failed[0] = true;
                        stream.pause();
                        promise.tryFail(e);
                    }
                })
                .endHandler(v -> {
                    if (failed[0]) return;
                    try {
                        b64Out.close();
                        promise.tryComplete(byteOut.toString("UTF-8"));
                    } catch (Exception e) {
                        promise.tryFail(e);
                    }
                })
                .exceptionHandler(e -> {
                    if (failed[0]) return;
                    failed[0] = true;
                    promise.tryFail(e);
                })
                .read();

        return promise.future();
    }

    // ══════════════════════════════════════════════
    //  Tool Call 构建
    // ══════════════════════════════════════════════

    private static void appendToolTurn(JsonObject obj, List<ChatCompletionMessageParam> result) {
        JsonObject assistantMsg = new JsonObject()
                .put("role", "assistant")
                .put("tool_calls", new JsonArray().add(buildToolCall(obj)));
        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMsg));
        result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(obj)));
    }

    private static JsonObject buildToolCall(JsonObject obj) {
        String id = obj.getString("id", "");
        String name = obj.getString("toolName", "");
        String arguments = obj.getString("functionArguments", "{}");
        return new JsonObject()
                .put("id", id)
                .put("type", "function")
                .put("function", new JsonObject().put("name", name).put("arguments", arguments));
    }

    private static JsonObject buildToolMessage(JsonObject obj) {
        String id = obj.getString("id", "");
        Object rawContent = obj.getValue("content");
        String content = rawContent == null ? ""
                : rawContent instanceof String s ? s
                  : rawContent.toString();
        return new JsonObject()
                .put("role", "tool")
                .put("tool_call_id", id)
                .put("content", content);
    }

    // ══════════════════════════════════════════════
    //  工具方法
    // ══════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private static JsonArray safeGetJsonArray(JsonObject obj, String key) {
        if (obj == null) return null;
        Object val = obj.getValue(key);
        if (val == null) return null;
        if (val instanceof JsonArray ja) return ja;
        if (val instanceof List<?> list) return new JsonArray(new ArrayList<>(list));
        return null;
    }

    /**
     * 将 contents 转为 JsonObject 列表（直接收集，不用 Stream 中间态）。
     */
    private static List<JsonObject> toJsonObjectList(List<Object> contents) {
        if (contents == null || contents.isEmpty()) return List.of();
        List<JsonObject> list = new ArrayList<>(contents.size());
        for (Object item : contents) {
            if (item == null) continue;
            JsonObject jo = toJsonObject(item);
            if (jo != null) list.add(jo);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static JsonObject toJsonObject(Object obj) {
        if (obj instanceof JsonObject jo) return jo;
        if (obj instanceof Map<?, ?> map) return new JsonObject((Map<String, Object>) map);
        return null;
    }

    private static List<Object> toList(JsonArray jsonArray) {
        return jsonArray == null ? List.of() : jsonArray.getList();
    }

    private static ContentTypeConstants parseType(JsonObject obj) {
        if (obj == null) return null;
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

    /**
     * 从 URL 中提取文件 ID。
     * 修复：先去除 query string，防止把 ?token=xxx 等参数带入 ID。
     */
    private static String extractFileId(String url) {
        if (isBlank(url)) return null;
        // 去掉 query string 和 fragment
        int queryIdx = url.indexOf('?');
        if (queryIdx >= 0) url = url.substring(0, queryIdx);
        int fragIdx = url.indexOf('#');
        if (fragIdx >= 0) url = url.substring(0, fragIdx);

        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == url.length() - 1) return null;
        return url.substring(lastSlash + 1);
    }

    private static String getExtension(String fileName) {
        if (isBlank(fileName)) return "octet-stream";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "octet-stream";
        return fileName.substring(dot + 1);
    }

    private static boolean hasFiles(JsonObject obj) {
        JsonArray images = safeGetJsonArray(obj, "images");
        if (images != null && !images.isEmpty()) return true;
        JsonArray videos = safeGetJsonArray(obj, "videos");
        if (videos != null && !videos.isEmpty()) return true;
        JsonArray files = safeGetJsonArray(obj, "files");
        return files != null && !files.isEmpty();
    }
}