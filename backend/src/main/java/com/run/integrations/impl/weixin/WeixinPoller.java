package com.run.integrations.impl.weixin;

import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.entity.Integration;
import com.run.dao.mapper.FileMapper;
import com.run.handler.integration.IIntegrationMessageDispatcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 单个微信账号长轮询: 收消息(含媒体入参) -> 跑应用 -> 按类型拆分回复(文本分块 + 图片/视频原生媒体) }
 */
public class WeixinPoller {

    private static final int SESSION_EXPIRED = -14;
    private static final int MAX_FAILURES = 3;

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "webm", "mkv");
    private static final Set<String> FILE_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "csv", "txt", "json", "md", "xml", "zip", "rar", "7z", "gz", "tar");
    private static final Pattern IMAGE_MD = Pattern.compile("!\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern LINK_MD = Pattern.compile("(?<!!)\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern VIDEO_TAG = Pattern.compile("(?s)<video[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*>(?:.*?</video>)?");
    private static final Pattern BARE_URL = Pattern.compile("(?<![(\\[\"'])https?://[^\\s)\\]]+");
    // 内部存储引用 ./api/storage/file/{uuid} 或 /api/storage/file/{uuid}
    private static final Pattern STORAGE_REF = Pattern.compile(
            "(?:\\./|/)?api/storage/file/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private final Integration integration;
    private final String baseUrl;
    private final String token;
    private final String accountId;
    private final IIntegrationMessageDispatcher dispatcher;
    private final FileMapper fileMapper;
    private final Vertx vertx;

    private volatile boolean running = false;
    private volatile Thread thread;
    private volatile String syncBuf = "";
    private final Map<String, String> contextTokens = new ConcurrentHashMap<>();
    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private static final long DEDUP_TTL_MS = 5 * 60 * 1000L;

    // 出站按用户串行化: 同一用户的文本/媒体/文件严格按入队顺序逐条发送, 避免并发乱序与竞态(尤其文件上传较慢时)。
    // 每条任务仍在独立虚拟线程上执行, 队列空闲不占用线程。
    private static final java.util.concurrent.Executor SEND_EXECUTOR =
            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, CompletableFuture<Void>> sendChains = new ConcurrentHashMap<>();

    public WeixinPoller(Integration integration, String baseUrl, String token, String accountId,
                        IIntegrationMessageDispatcher dispatcher, FileMapper fileMapper, Vertx vertx) {
        this.integration = integration;
        this.baseUrl = baseUrl;
        this.token = token;
        this.accountId = accountId;
        this.dispatcher = dispatcher;
        this.fileMapper = fileMapper;
        this.vertx = vertx;
    }

    public void start() {
        running = true;
        thread = Thread.ofVirtual().name("weixin-poll-" + accountId).start(this::loop);
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        int fails = 0;
        while (running) {
            try {
                JsonObject resp = IlinkClient.getUpdates(baseUrl, token, syncBuf);
                int ret = resp.getInteger("ret", 0);
                int errcode = resp.getInteger("errcode", 0);
                if (ret != 0 || errcode != 0) {
                    if (ret == SESSION_EXPIRED || errcode == SESSION_EXPIRED) {
                        System.err.println("[weixin] session expired, pause 10min; account=" + accountId);
                        sleep(600_000);
                        continue;
                    }
                    fails++;
                    sleep(fails >= MAX_FAILURES ? 30_000 : 2_000);
                    if (fails >= MAX_FAILURES) {
                        fails = 0;
                    }
                    continue;
                }
                fails = 0;
                String buf = resp.getString("get_updates_buf", "");
                if (!buf.isEmpty()) {
                    syncBuf = buf;
                }
                JsonArray msgs = resp.getJsonArray("msgs");
                int n = msgs == null ? 0 : msgs.size();
                if (n > 0) {
                    System.out.println("[weixin] poll msgs=" + n);
                }
                if (msgs != null) {
                    for (int i = 0; i < msgs.size(); i++) {
                        JsonObject msg = msgs.getJsonObject(i);
                        Thread.ofVirtual().start(() -> handle(msg));
                    }
                }
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                System.err.println("[weixin] poll exception: " + e.getClass().getName() + ": " + e.getMessage());
                fails++;
                sleep(fails >= MAX_FAILURES ? 30_000 : 2_000);
                if (fails >= MAX_FAILURES) {
                    fails = 0;
                }
            }
        }
    }

    private void handle(JsonObject msg) {
        try {
            String sender = msg.getString("from_user_id", "").strip();
            if (sender.isEmpty() || sender.equals(accountId)) {
                return;
            }
            // 去重: iLink 在 syncBuf 推进前可能重复推送同一消息; 按 发送者+内容哈希 + 5min 窗口去重
            String itemsJson = msg.getJsonArray("item_list", new JsonArray()).encode();
            String dedupKey = sender + ":" + Aes128Ecb.md5Hex(itemsJson.getBytes(StandardCharsets.UTF_8));
            if (isDuplicate(dedupKey)) {
                return;
            }
            String ctx = msg.getString("context_token", "");
            if (!ctx.isEmpty()) {
                contextTokens.put(sender, ctx);
            }
            JsonObject content = buildContent(msg.getJsonArray("item_list"));
            if (content == null) {
                return;
            }
            // 按内容块 id 分段: 每完成一段(TEXT)就立刻发该段的文本+媒体, "给一点发一点"
            java.util.concurrent.atomic.AtomicBoolean anySent = new java.util.concurrent.atomic.AtomicBoolean(false);
            // 同一回答内已发出的媒体(绝对 URL / storage fileId): 跨 segment 去重, 避免一张图/一个文件被多段重复发送
            Set<String> sentMedia = ConcurrentHashMap.newKeySet();
            dispatcher.dispatchSegments(integration, sender, content, (type, seg) -> {
                        System.out.println("[weixin-seg] recv type=" + type + " len=" + (seg == null ? -1 : seg.length()));
                        if ("TEXT".equals(type) || "REASONING".equals(type)) {
                            anySent.set(true);
                            enqueueSend(sender, () -> sendSegment(sender, seg, sentMedia));
                        } else if ("TOOL".equals(type)) {
                            anySent.set(true);
                            enqueueSend(sender, () -> sendTextSafe(sender, seg));
                        }
                    })
                    .onSuccess(reply -> {
                        // ⚠️ 该回调在 Vert.x event loop 线程上执行; sendText 是阻塞 HTTP, 不能直接调。
                        // enqueueSend 会把任务丢到串行队列(虚拟线程)上跑, 既不卡事件循环又保证排在已有分段之后。
                        if (!anySent.get()) {
                            enqueueSend(sender, () -> sendTextSafe(sender, "(无回复)"));
                        }
                    })
                    .onFailure(e -> {
                        System.err.println("[weixin] dispatch failed: " + e.getMessage());
                        enqueueSend(sender, () -> sendTextSafe(sender, "⚠️ 处理失败: " + e.getMessage()));
                    });
        } catch (Exception e) {
            System.err.println("[weixin] handle error: " + e.getMessage());
        }
    }

    /**
     * 把发送任务追加到该用户串行队列尾部, 保证逐条按入队顺序发送。
     * 用 handleAsync 串链: 即使前一条异常也继续跑下一条; 任务内吞掉异常, 避免链断裂导致后续消息不发。
     */
    private void enqueueSend(String toUser, Runnable task) {
        sendChains.compute(toUser, (k, tail) -> {
            CompletableFuture<Void> base = tail == null ? CompletableFuture.completedFuture(null) : tail;
            return base.handleAsync((v, ex) -> {
                try {
                    task.run();
                } catch (Throwable t) {
                    System.err.println("[weixin] send task error: " + t.getMessage());
                }
                return null;
            }, SEND_EXECUTOR);
        });
    }

    // ==================== 入站: 文本 + 媒体入参 ====================

    private JsonObject buildContent(JsonArray itemList) {
        if (itemList == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        JsonArray images = new JsonArray();
        JsonArray videos = new JsonArray();
        JsonArray files = new JsonArray();
        for (int i = 0; i < itemList.size(); i++) {
            JsonObject item = itemList.getJsonObject(i);
            int type = item.getInteger("type", 0);
            try {
                if (type == IlinkClient.ITEM_TEXT) {
                    text.append(item.getJsonObject("text_item", new JsonObject()).getString("text", ""));
                } else if (type == IlinkClient.ITEM_IMAGE) {
                    images.add(storeInbound(item, "image_item", "image.jpg"));
                } else if (type == IlinkClient.ITEM_VIDEO) {
                    videos.add(storeInbound(item, "video_item", "video.mp4"));
                } else if (type == IlinkClient.ITEM_FILE) {
                    String name = item.getJsonObject("file_item", new JsonObject()).getString("file_name", "file.bin");
                    files.add(storeInbound(item, "file_item", name));
                } else if (type == IlinkClient.ITEM_VOICE) {
                    files.add(storeInbound(item, "voice_item", "voice.silk"));
                }
            } catch (Exception e) {
                System.err.println("[weixin] inbound media failed: " + e.getMessage());
            }
        }
        if (text.length() == 0 && images.isEmpty() && videos.isEmpty() && files.isEmpty()) {
            return null;
        }
        JsonObject content = new JsonObject().put("content", text.toString());
        if (!images.isEmpty()) {
            content.put("images", images);
        }
        if (!videos.isEmpty()) {
            content.put("videos", videos);
        }
        if (!files.isEmpty()) {
            content.put("files", files);
        }
        return content;
    }

    private JsonObject storeInbound(JsonObject item, String subKey, String name) throws Exception {
        byte[] data = WeixinMedia.downloadInbound(item, subKey);
        java.io.File tmp = Files.createTempFile("weixin-", ".bin").toFile();
        Files.write(tmp.toPath(), data);
        var entity = fileMapper.upload(name, tmp.length(), null, null, tmp)
                .toCompletionStage().toCompletableFuture().get();
        return new JsonObject().put("url", "./api/storage/file/" + entity.getId()).put("name", name);
    }

    // ==================== 出站: 按类型拆分 ====================

    /**
     * 发送一段(TEXT 块): 文本整段发(一段=一条), 段内绝对 URL/内部引用的图片/视频/文件走原生媒体。
     * sentMedia: 同一回答内已发媒体的去重集合(跨 segment), 保证同一资源在一个回答里只发一次。
     */
    private void sendSegment(String toUser, String seg, Set<String> sentMedia) {
        String ctx = contextTokens.get(toUser);
        List<String> images = new ArrayList<>();
        List<String> videos = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> storageIds = new ArrayList<>();
        collectMedia(seg, images, videos, files, storageIds);
        String text = stripAbsMedia(seg);
        System.out.println("[weixin-seg] sendSegment segLen=" + seg.length() + " textLen=" + text.length()
                + " blank=" + text.isBlank() + " images=" + images.size() + " videos=" + videos.size()
                + " files=" + files.size() + " storageIds=" + storageIds.size());
        try {
            if (!text.isBlank()) {
                JsonObject resp = IlinkClient.sendText(baseUrl, token, toUser, text, ctx, UUID.randomUUID().toString());
                System.out.println("[weixin-seg] sendText ret=" + resp.getInteger("ret", 0)
                        + " errcode=" + resp.getInteger("errcode", 0) + " errmsg=" + resp.getString("errmsg", ""));
            }
            for (String url : images) {
                if (sentMedia.add("url:" + url)) {
                    sendMedia(toUser, url, "image", ctx);
                }
            }
            for (String url : videos) {
                if (sentMedia.add("url:" + url)) {
                    sendMedia(toUser, url, "video", ctx);
                }
            }
            for (String url : files) {
                if (sentMedia.add("url:" + url)) {
                    sendMedia(toUser, url, "file", ctx);
                }
            }
            for (String id : storageIds) {
                if (sentMedia.add("storage:" + id)) {
                    sendStorageMedia(toUser, id, ctx);
                }
            }
        } catch (Exception e) {
            System.err.println("[weixin] send segment failed: " + e.getMessage());
        }
    }

    private boolean isDuplicate(String key) {
        long now = System.currentTimeMillis();
        seen.entrySet().removeIf(e -> now - e.getValue() > DEDUP_TTL_MS);
        return seen.putIfAbsent(key, now) != null;
    }

    private void sendTextSafe(String toUser, String text) {
        try {
            IlinkClient.sendText(baseUrl, token, toUser, text, contextTokens.get(toUser), UUID.randomUUID().toString());
        } catch (Exception e) {
            System.err.println("[weixin] send text failed: " + e.getMessage());
        }
    }

    /**
     * 从文本提取绝对 URL 媒体: 图片/视频/文件(按后缀分类)。
     * ![](url) 默认图片; [文字](url) 和裸链接只取视频/文件(避免抓网页/重复图片)。
     */
    private void collectMedia(String text, List<String> images, List<String> videos, List<String> files,
                             List<String> storageIds) {
        Set<String> seenUrls = new java.util.HashSet<>();
        Matcher vt = VIDEO_TAG.matcher(text);
        while (vt.find()) {
            add(vt.group(1), "video", images, videos, files, seenUrls);
        }
        Matcher im = IMAGE_MD.matcher(text);
        while (im.find()) {
            String url = im.group(1);
            if (!collectStorage(url, storageIds, seenUrls)) {
                add(url, orElse(classify(url), "image"), images, videos, files, seenUrls);
            }
        }
        Matcher lm = LINK_MD.matcher(text);
        while (lm.find()) {
            String url = lm.group(1);
            if (!collectStorage(url, storageIds, seenUrls)) {
                collectVideoOrFile(url, images, videos, files, seenUrls);
            }
        }
        Matcher bu = BARE_URL.matcher(text);
        while (bu.find()) {
            collectVideoOrFile(bu.group(), images, videos, files, seenUrls);
        }
        // 文本里直接出现的内部存储引用(未包在 markdown 里)
        Matcher sr = STORAGE_REF.matcher(text);
        while (sr.find()) {
            if (seenUrls.add(sr.group())) {
                storageIds.add(sr.group(1));
            }
        }
    }

    private boolean collectStorage(String url, List<String> storageIds, Set<String> seen) {
        Matcher m = STORAGE_REF.matcher(url);
        if (m.find()) {
            if (seen.add(url)) {
                storageIds.add(m.group(1));
            }
            return true;
        }
        return false;
    }

    private void collectVideoOrFile(String url, List<String> images, List<String> videos, List<String> files, Set<String> seen) {
        String k = classify(url);
        if ("video".equals(k) || "file".equals(k)) {
            add(url, k, images, videos, files, seen);
        }
    }

    private void add(String url, String kind, List<String> images, List<String> videos, List<String> files, Set<String> seen) {
        if (!isAbs(url) || !seen.add(url)) {
            return;
        }
        switch (kind) {
            case "video" -> videos.add(url);
            case "file" -> files.add(url);
            default -> images.add(url);
        }
    }

    /**
     * 去掉文本里的绝对 URL 媒体标记(已作为原生媒体发送); 相对地址/普通链接保留
     */
    private String stripAbsMedia(String text) {
        text = VIDEO_TAG.matcher(text).replaceAll(mr -> isAbs(mr.group(1)) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = IMAGE_MD.matcher(text).replaceAll(mr -> (isAbs(mr.group(1)) || isStorageRef(mr.group(1))) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = LINK_MD.matcher(text).replaceAll(mr -> (isSentLink(mr.group(1)) || isStorageRef(mr.group(1))) ? "" : Matcher.quoteReplacement(mr.group(0)));
        text = BARE_URL.matcher(text).replaceAll(mr -> isSentLink(mr.group()) ? "" : Matcher.quoteReplacement(mr.group()));
        text = STORAGE_REF.matcher(text).replaceAll("");
        return text.replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static boolean isStorageRef(String url) {
        return url != null && STORAGE_REF.matcher(url).find();
    }

    private static boolean isSentLink(String url) {
        String k = classify(url);
        return isAbs(url) && ("video".equals(k) || "file".equals(k));
    }

    private static String classify(String url) {
        String e = ext(url);
        if (IMAGE_EXT.contains(e)) {
            return "image";
        }
        if (VIDEO_EXT.contains(e)) {
            return "video";
        }
        if (FILE_EXT.contains(e)) {
            return "file";
        }
        return null;
    }

    private static String orElse(String s, String fallback) {
        return s == null ? fallback : s;
    }

    private static boolean isAbs(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * 内部存储引用 ./api/storage/file/{id}: 直接从 fileMapper 读字节(不走 HTTP), 按文件名后缀判类型发原生媒体
     */
    private void sendStorageMedia(String toUser, String fileId, String ctx) {
        String name = "file.bin";
        try {
            FileEntity fe = fileMapper.getById(fileId).toCompletionStage().toCompletableFuture().get();
            if (fe == null) {
                System.err.println("[weixin] send storage file: not found id=" + fileId);
                return;
            }
            name = orElse(fe.getFileName(), "file.bin");
            byte[] data = readStorageBytes(fe);
            String kind = orElse(classify(name), "file");
            JsonObject item = WeixinMedia.uploadAndBuildItem(baseUrl, token, toUser, data, name, kind);
            JsonObject resp = IlinkClient.sendItem(baseUrl, token, toUser, item, ctx, UUID.randomUUID().toString());
            if (!checkItemResp(resp, kind, name)) {
                // iLink 拒绝该媒体(文件类型/大小/session 等): 不能静默丢, 退化成文本告知用户
                sendTextSafe(toUser, "⚠️ 文件发送失败: " + name);
            }
        } catch (Exception e) {
            System.err.println("[weixin] send storage file failed: " + name + " -> " + e.getMessage());
            sendTextSafe(toUser, "⚠️ 文件发送失败: " + name);
        }
    }

    /**
     * 检查 iLink sendItem 返回码(媒体/文件路径之前完全忽略返回, 失败会静默丢失)。失败返回 false。
     */
    private static boolean checkItemResp(JsonObject resp, String kind, String name) {
        int ret = resp == null ? -1 : resp.getInteger("ret", 0);
        int errcode = resp == null ? -1 : resp.getInteger("errcode", 0);
        if (ret != 0 || errcode != 0) {
            System.err.println("[weixin] sendItem " + kind + " failed name=" + name + " ret=" + ret
                    + " errcode=" + errcode + " errmsg=" + (resp == null ? "" : resp.getString("errmsg", "")));
            return false;
        }
        return true;
    }

    private byte[] readStorageBytes(FileEntity fe) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BaseReadStream stream = fileMapper.downloadFile(vertx, fe);
        CompletableFuture<Void> done = new CompletableFuture<>();
        stream.handler(buf -> out.writeBytes(buf.getBytes()))
                .endHandler(v -> done.complete(null))
                .exceptionHandler(done::completeExceptionally);
        stream.read();
        done.get();
        return out.toByteArray();
    }

    private void sendMedia(String toUser, String url, String kind, String ctx) {
        String name = fileNameFromUrl(url, kind);
        try {
            byte[] data = IlinkClient.downloadBytes(url);
            JsonObject item = WeixinMedia.uploadAndBuildItem(baseUrl, token, toUser, data, name, kind);
            JsonObject resp = IlinkClient.sendItem(baseUrl, token, toUser, item, ctx, UUID.randomUUID().toString());
            if (!checkItemResp(resp, kind, name)) {
                // 发送失败: 退化成把原始链接作为文本发出去, 用户至少能拿到内容
                sendTextSafe(toUser, "📎 " + name + "\n" + url);
            }
        } catch (Exception e) {
            System.err.println("[weixin] send " + kind + " failed: " + name + " -> " + e.getMessage());
            sendTextSafe(toUser, "📎 " + name + "\n" + url);
        }
    }

    private static String ext(String url) {
        String path = url.split("[?#]")[0].toLowerCase();
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1);
    }

    private static String fileNameFromUrl(String url, String kind) {
        String path = url.split("[?#]")[0];
        int idx = path.lastIndexOf('/');
        String name = idx >= 0 ? path.substring(idx + 1) : path;
        if (name.isEmpty() || !name.contains(".")) {
            name = switch (kind) {
                case "video" -> "video.mp4";
                case "file" -> "file.bin";
                default -> "image.jpg";
            };
        }
        return name;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
