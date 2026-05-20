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

        ContentTypeConstants currentRole = null;
        StringBuilder buf = new StringBuilder();
        // 缓存相邻且 aiId 相同的 TOOL 消息
        List<JsonObject> toolGroup = new ArrayList<>();

        for (JsonObject obj : toJsonObjectList(contents)) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) continue;

            String extracted = cfg.extract(type, obj);

            if (type == ContentTypeConstants.TOOL) {
                // 先 flush 非 TOOL 缓冲
                flushBuffer(currentRole, buf, result);
                currentRole = null;

                if (!toolGroup.isEmpty() && !sameAiId(toolGroup.get(0), obj)) {
                    // aiId 不同，先把之前的 group flush 掉
                    appendToolGroup(toolGroup, result);
                    toolGroup.clear();
                }
                toolGroup.add(obj);
                continue;
            }

            // 遇到非 TOOL，先 flush 积攒的 toolGroup
            if (!toolGroup.isEmpty()) {
                appendToolGroup(toolGroup, result);
                toolGroup.clear();
            }

            if (type != currentRole) {
                flushBuffer(currentRole, buf, result);
                currentRole = type;
            }

            if (!isBlank(extracted)) {
                buf.append(extracted);
            }
        }

        // 尾部 flush
        if (!toolGroup.isEmpty()) {
            appendToolGroup(toolGroup, result);
            toolGroup.clear();
        }
        flushBuffer(currentRole, buf, result);
        return result;
    }

    private static void flushBuffer(ContentTypeConstants role, StringBuilder buf,
                                    List<ChatCompletionMessageParam> result) {
        if (role == null || buf.length() == 0) return;
        switch (role) {
            case SYSTEM -> flushSystem(buf, result);
            case QUESTION -> flushUser(buf, result);
            case TEXT -> flushAssistantText(buf, result);
            default -> buf.setLength(0);
        }
    }

    // ══════════════════════════════════════════════
    //  异步核心转换（支持 QUESTION 多模态）
    // ══════════════════════════════════════════════

    private static Future<List<ChatCompletionMessageParam>> convertAsync(List<Object> contents,
                                                                         ContentConvertConfig config,
                                                                         FileMapper fileMapper,
                                                                         Vertx vertx) {
        ContentConvertConfig cfg = config == null ? ContentConvertConfig.defaultConfig() : config;

        List<ChatCompletionMessageParam> result = new ArrayList<>();
        StringBuilder systemBuf = new StringBuilder();
        StringBuilder userTextBuf = new StringBuilder();
        List<JsonObject> userFileParts = new ArrayList<>();
        StringBuilder assistantTextBuf = new StringBuilder();
        // 缓存相邻且 aiId 相同的 TOOL 消息
        List<JsonObject> toolGroup = new ArrayList<>();

        Future<Void> chain = Future.succeededFuture();

        for (JsonObject obj : toJsonObjectList(contents)) {
            ContentTypeConstants type = parseType(obj);
            if (type == null) continue;

            String extracted = cfg.extract(type, obj);

            switch (type) {
                case SYSTEM -> {
                    chain = chain.compose(v -> {
                        if (!toolGroup.isEmpty()) {
                            appendToolGroup(toolGroup, result);
                            toolGroup.clear();
                        }
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        flushAssistantText(assistantTextBuf, result);
                        if (!isBlank(extracted)) {
                            systemBuf.append(extracted);
                        }
                        return Future.<Void>succeededFuture();
                    });
                }
                case QUESTION -> {
                    boolean needReadFiles = hasFiles(obj);
                    chain = chain.compose(v -> {
                        if (!toolGroup.isEmpty()) {
                            appendToolGroup(toolGroup, result);
                            toolGroup.clear();
                        }
                        flushSystem(systemBuf, result);
                        flushAssistantText(assistantTextBuf, result);
                        if (!isBlank(extracted)) {
                            userTextBuf.append(extracted);
                        }
                        if (needReadFiles) {
                            return readQuestionFileParts(obj, fileMapper, vertx)
                                    .compose(parts -> {
                                        if (parts != null && !parts.isEmpty()) {
                                            userFileParts.addAll(parts);
                                        }
                                        return Future.<Void>succeededFuture();
                                    });
                        }
                        return Future.<Void>succeededFuture();
                    });
                }
                case TEXT -> {
                    chain = chain.compose(v -> {
                        if (!toolGroup.isEmpty()) {
                            appendToolGroup(toolGroup, result);
                            toolGroup.clear();
                        }
                        flushSystem(systemBuf, result);
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        if (!isBlank(extracted)) {
                            assistantTextBuf.append(extracted);
                        }
                        return Future.<Void>succeededFuture();
                    });
                }
                case TOOL -> {
                    chain = chain.compose(v -> {
                        flushSystem(systemBuf, result);
                        flushUserFromParts(userTextBuf, userFileParts, result);
                        flushAssistantText(assistantTextBuf, result);
                        // 如果 aiId 不同，先 flush 之前的 group
                        if (!toolGroup.isEmpty() && !sameAiId(toolGroup.get(0), obj)) {
                            appendToolGroup(toolGroup, result);
                            toolGroup.clear();
                        }
                        toolGroup.add(obj);
                        return Future.<Void>succeededFuture();
                    });
                }
            }
        }

        return chain.compose(v -> {
            if (!toolGroup.isEmpty()) {
                appendToolGroup(toolGroup, result);
                toolGroup.clear();
            }
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

    private static void flushUserFromParts(StringBuilder textBuf,
                                           List<JsonObject> fileParts,
                                           List<ChatCompletionMessageParam> result) {
        if (textBuf.length() == 0 && fileParts.isEmpty()) return;

        if (fileParts.isEmpty()) {
            flushUser(textBuf, result);
            return;
        }

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

    /**
     * 读取文件并构造多模态 content part。
     * <p>
     * 支持两种 url 格式：
     * <ul>
     *   <li>绝对路径（以 "/" 开头）：直接通过 Vert.x FileSystem 读取本地文件</li>
     *   <li>相对 API 路径（如 "./api/storage/file/:id"）：通过 fileMapper 读取</li>
     * </ul>
     * <p>
     * 如果文件不存在或读取失败，静默返回 null，不中断整条 Future 链。
     */
    private static Future<JsonObject> readFileAsContentPart(JsonObject fileObj,
                                                            String urlType,
                                                            String mimeTypePrefix,
                                                            FileMapper fileMapper,
                                                            Vertx vertx) {
        try {
            String url = fileObj.getString("url", "");
            if (isBlank(url)) {
                return Future.succeededFuture();
            }

            if (url.startsWith("/")) {
                // ── 绝对路径：直接读本地文件系统 ──
                Future<JsonObject> f = vertx.fileSystem().readFile(url)
                        .map(buffer -> {
                            String fileName = url.substring(url.lastIndexOf('/') + 1);
                            String mime = MimeMapping.mimeTypeForFilename(fileName);
                            if (mime == null || !mime.startsWith(mimeTypePrefix)) {
                                mime = mimeTypePrefix + "/" + getExtension(fileName);
                            }
                            String base64 = Base64.getEncoder().encodeToString(buffer.getBytes());
                            String dataUrl = "data:" + mime + ";base64," + base64;
                            return (JsonObject) new JsonObject()
                                    .put("type", urlType)
                                    .put(urlType, new JsonObject().put("url", dataUrl));
                        });
                return f != null ? f.otherwise(err -> null) : Future.succeededFuture();
            }

            // ── 相对 API 路径：通过 fileMapper 读取 ──
            String fileId = extractFileId(url);
            if (isBlank(fileId)) {
                return Future.succeededFuture();
            }

            Future<? extends FileEntity> getByIdFuture = fileMapper.getById(fileId);
            // ★ 防御 fileMapper.getById 直接返回 null
            if (getByIdFuture == null) {
                return Future.succeededFuture();
            }

            Future<JsonObject> resultFuture = getByIdFuture
                    .compose(entity -> {
                        // ★ 数据库中无此记录，跳过
                        if (entity == null) {
                            return Future.succeededFuture((JsonObject) null);
                        }
                        return readBase64(fileMapper, entity, vertx)
                                .map(base64 -> {
                                    String fileName = entity.getFileName();
                                    String mime = MimeMapping.mimeTypeForFilename(fileName);
                                    if (mime == null || !mime.startsWith(mimeTypePrefix)) {
                                        mime = mimeTypePrefix + "/" + getExtension(fileName);
                                    }
                                    String dataUrl = "data:" + mime + ";base64," + base64;
                                    return (JsonObject) new JsonObject()
                                            .put("type", urlType)
                                            .put(urlType, new JsonObject().put("url", dataUrl));
                                });
                    })
                    // ★ 任何异常（getById 失败、readBase64 失败等）都静默跳过
                    .otherwise(err -> null);
            return resultFuture;
        } catch (Exception e) {
            // ★ 兜底：fileMapper 方法抛同步异常、返回 null 等任何意外情况
            return Future.succeededFuture();
        }
    }

    /**
     * 将文件流读取为 Base64 字符串。
     */
    private static Future<String> readBase64(FileMapper fileMapper, FileEntity entity, Vertx vertx) {
        Promise<String> promise = Promise.promise();
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            OutputStream b64Out = Base64.getEncoder().wrap(byteOut);
            boolean[] failed = {false};

            var stream = fileMapper.downloadFile(vertx, entity);
            if (stream == null) {
                return Future.failedFuture("downloadFile returned null for: " + entity.getFileName());
            }
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
        } catch (Exception e) {
            promise.tryFail(e);
        }

        return promise.future();
    }

    // ══════════════════════════════════════════════
    //  Tool Call 构建（支持 aiId 合并 + reasoning）
    // ══════════════════════════════════════════════

    /**
     * 将一组相邻且 aiId 相同的 TOOL 消息合并为：
     * <ol>
     *   <li>一条 assistant 消息（含 tool_calls，可选 reasoning 和 content）</li>
     *   <li>每个 TOOL 对应一条 tool result 消息</li>
     * </ol>
     */
    private static void appendToolGroup(List<JsonObject> group, List<ChatCompletionMessageParam> result) {
        if (group == null || group.isEmpty()) return;

        // ── 构建 assistant 消息 ──
        JsonObject assistantMsg = new JsonObject().put("role", "assistant");

        // tool_calls 数组
        JsonArray toolCalls = new JsonArray();
        for (JsonObject obj : group) {
            toolCalls.add(buildToolCall(obj));
        }
        assistantMsg.put("tool_calls", toolCalls);

        // reasoning：取 group 中第一个带 reasoningKey 的（同一次 AI 回复共享 reasoning）
        // 只要 reasoningKey 非空就加字段，reasoning 为空字符串也保留
        String reasoning = null;
        String reasoningKey = null;
        for (JsonObject obj : group) {
            reasoningKey = obj.getString("reasoningKey");
            if (!isBlank(reasoningKey)) {
                reasoning = obj.getString("reasoning", "");
                break;
            }
        }
        if (!isBlank(reasoningKey)) {
            assistantMsg.put(reasoningKey, reasoning != null ? reasoning : "");
        }

        result.add(ChatCompletionMessageParam.fromJsonObject(assistantMsg));

        // ── 构建 tool result 消息 ──
        for (JsonObject obj : group) {
            result.add(ChatCompletionMessageParam.fromJsonObject(buildToolMessage(obj)));
        }
    }

    /**
     * 判断两个 TOOL 消息的 aiId 是否相同。
     * aiId 为空时视为不同（不合并）。
     */
    private static boolean sameAiId(JsonObject a, JsonObject b) {
        if (a == null || b == null) return false;
        String aiIdA = a.getString("aiId");
        String aiIdB = b.getString("aiId");
        if (isBlank(aiIdA) || isBlank(aiIdB)) return false;
        return aiIdA.equals(aiIdB);
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

    private static String extractFileId(String url) {
        if (isBlank(url)) return null;
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