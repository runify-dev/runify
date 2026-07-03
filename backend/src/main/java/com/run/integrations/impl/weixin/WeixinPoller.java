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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    // sendmessage ret=-2 且 errmsg 为空/unknown error => context_token 失效(见 hermes #17228/#18100), 真限流的 errmsg 会带 frequency 文案
    private static final int RET_STALE_SESSION = -2;
    // 每用户暂存补发队列上限, 超出丢最旧的
    private static final int MAX_PENDING = 50;
    // 单条文本上限(字符): 1200 字中文约 3.6KB, 低于 iLink 单条文本 ~4KB 上限; 超长整条会被拒收
    private static final int MAX_TEXT_CHARS = 1200;
    // 同一用户两次发送的最小间隔, 避免分段回复背靠背触发频控
    private static final long MIN_SEND_INTERVAL_MS = 800;

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

    // 会话失效的用户集合: 命中后该用户的出站消息直接进 pending, 不再逐条白发; 收到新 context_token 时解除并补发。
    // pending/lastSendAt 只在该用户的发送链(串行)上读写, 队列本身无并发。
    private final Set<String> staleSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Outbound>> pending = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSendAt = new ConcurrentHashMap<>();

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
                // 新 ctx 到手: 解除会话失效标记, 上一轮被拒暂存的消息排在本轮回复之前按序补发
                staleSessions.remove(sender);
                if (pending.containsKey(sender)) {
                    enqueueSend(sender, () -> replayPending(sender));
                }
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
                        // TOOL 通知("🔧 调用工具 xxx")不单独发: iLink 每个 context_token 回复条数有限,
                        // 一次 agent 回答动辄 8~9 个工具调用, 通知会把配额烧光, 导致后面的正文/文件全被拒(ret=-2)
                        if ("TEXT".equals(type) || "REASONING".equals(type)) {
                            anySent.set(true);
                            enqueueSend(sender, () -> sendSegment(sender, seg, sentMedia));
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
     * 发送一段(TEXT 块): 文本整段发(超长自动切分), 段内绝对 URL/内部引用的图片/视频/文件走原生媒体。
     * sentMedia: 同一回答内已发媒体的去重集合(跨 segment), 保证同一资源在一个回答里只发一次。
     */
    private void sendSegment(String toUser, String seg, Set<String> sentMedia) {
        List<String> images = new ArrayList<>();
        List<String> videos = new ArrayList<>();
        List<String> files = new ArrayList<>();
        List<String> storageIds = new ArrayList<>();
        collectMedia(seg, images, videos, files, storageIds);
        String text = stripAbsMedia(seg);
        System.out.println("[weixin-seg] sendSegment segLen=" + seg.length() + " textLen=" + text.length()
                + " images=" + images.size() + " videos=" + videos.size()
                + " files=" + files.size() + " storageIds=" + storageIds.size());
        if (!text.isBlank()) {
            submit(toUser, Outbound.text(text));
        }
        for (String url : images) {
            if (sentMedia.add("url:" + url)) {
                submit(toUser, Outbound.media(url, "image"));
            }
        }
        for (String url : videos) {
            if (sentMedia.add("url:" + url)) {
                submit(toUser, Outbound.media(url, "video"));
            }
        }
        for (String url : files) {
            if (sentMedia.add("url:" + url)) {
                submit(toUser, Outbound.media(url, "file"));
            }
        }
        for (String id : storageIds) {
            if (sentMedia.add("storage:" + id)) {
                submit(toUser, Outbound.storage(id));
            }
        }
    }

    private boolean isDuplicate(String key) {
        long now = System.currentTimeMillis();
        seen.entrySet().removeIf(e -> now - e.getValue() > DEDUP_TTL_MS);
        return seen.putIfAbsent(key, now) != null;
    }

    private void sendTextSafe(String toUser, String text) {
        submit(toUser, Outbound.text(text));
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

    // ==================== 出站: 统一发送出口(会话失效可暂存补发) ====================

    /**
     * 一条出站消息(文本 / 外链媒体 / 内部存储媒体)。媒体只存来源引用不存字节,
     * 补发时整条重做(重新下载/加密/上传), 避免暂存大对象和过期的上传凭证。
     */
    private record Outbound(Kind kind, String text, String url, String mediaKind, String storageId) {
        enum Kind {TEXT, URL_MEDIA, STORAGE_MEDIA}

        static Outbound text(String text) {
            return new Outbound(Kind.TEXT, text, null, null, null);
        }

        static Outbound media(String url, String mediaKind) {
            return new Outbound(Kind.URL_MEDIA, null, url, mediaKind, null);
        }

        static Outbound storage(String id) {
            return new Outbound(Kind.STORAGE_MEDIA, null, null, null, id);
        }
    }

    /**
     * 发送入口: 该用户会话已标记失效时直接暂存(不再逐条白发); 否则立即尝试,
     * 发送中命中会话失效由 deliver 自行暂存未送达部分。任何路径都不抛异常。
     */
    private void submit(String toUser, Outbound out) {
        if (staleSessions.contains(toUser)) {
            park(toUser, out);
        } else {
            deliver(toUser, out);
        }
    }

    private void deliver(String toUser, Outbound out) {
        switch (out.kind()) {
            case TEXT -> deliverText(toUser, out.text());
            case URL_MEDIA -> deliverUrlMedia(toUser, out);
            case STORAGE_MEDIA -> deliverStorageMedia(toUser, out);
        }
    }

    /**
     * 文本(超长先切分逐条发)。会话失效: 从失败的那条起合并暂存, 已发出的不重发;
     * 非会话类拒绝: 记日志丢弃继续, 避免毒消息永久卡住补发队列。
     */
    private void deliverText(String toUser, String text) {
        List<String> parts = splitText(text);
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            boolean ok = sendViaIlink(toUser, "text",
                    ctx -> IlinkClient.sendText(baseUrl, token, toUser, part, ctx, UUID.randomUUID().toString()));
            if (!ok && staleSessions.contains(toUser)) {
                park(toUser, Outbound.text(String.join("\n", parts.subList(i, parts.size()))));
                return;
            }
        }
    }

    private void deliverUrlMedia(String toUser, Outbound out) {
        String name = fileNameFromUrl(out.url(), out.mediaKind());
        try {
            byte[] data = IlinkClient.downloadBytes(out.url());
            deliverItem(toUser, data, name, out.mediaKind(), out, "📎 " + name + "\n" + out.url());
        } catch (Exception e) {
            System.err.println("[weixin] download " + out.mediaKind() + " failed: " + name + " -> " + e.getMessage());
            // 拿不到字节: 退化成把原始链接发出去, 用户至少能拿到内容
            deliverText(toUser, "📎 " + name + "\n" + out.url());
        }
    }

    /**
     * 内部存储引用 ./api/storage/file/{id}: 直接从 fileMapper 读字节(不走 HTTP), 按文件名后缀判类型发原生媒体
     */
    private void deliverStorageMedia(String toUser, Outbound out) {
        String name = "file.bin";
        try {
            FileEntity fe = fileMapper.getById(out.storageId()).toCompletionStage().toCompletableFuture().get();
            if (fe == null) {
                System.err.println("[weixin] send storage file: not found id=" + out.storageId());
                return;
            }
            name = orElse(fe.getFileName(), "file.bin");
            byte[] data = readStorageBytes(fe);
            deliverItem(toUser, data, name, orElse(classify(name), "file"), out, "⚠️ 文件发送失败: " + name);
        } catch (Exception e) {
            System.err.println("[weixin] send storage file failed: " + name + " -> " + e.getMessage());
            deliverText(toUser, "⚠️ 文件发送失败: " + name);
        }
    }

    /**
     * 加密上传 CDN 并发送媒体 item。会话失效: 暂存原始 Outbound(补发时整条重做);
     * 其他拒绝/异常: 降级为 fallback 文本, 不能静默丢。
     */
    private void deliverItem(String toUser, byte[] data, String name, String kind, Outbound origin, String fallback) {
        try {
            JsonObject item = WeixinMedia.uploadAndBuildItem(baseUrl, token, toUser, data, name, kind);
            boolean ok = sendViaIlink(toUser, kind + " " + name,
                    ctx -> IlinkClient.sendItem(baseUrl, token, toUser, item, ctx, UUID.randomUUID().toString()));
            if (ok) {
                return;
            }
            if (staleSessions.contains(toUser)) {
                park(toUser, origin);
            } else {
                deliverText(toUser, fallback);
            }
        } catch (Exception e) {
            System.err.println("[weixin] send " + kind + " failed: " + name + " -> " + e.getMessage());
            deliverText(toUser, fallback);
        }
    }

    private interface IlinkSend {
        JsonObject run(String ctx) throws Exception;
    }

    /**
     * 统一 sendmessage 出口: 限速后带当前 ctx 发送; 命中会话失效信号时去掉 ctx 重试一次(tokenless retry),
     * 仍失败则标记该用户会话失效(后续消息转入 pending, 等新 ctx 补发)。返回是否被服务端接受。
     */
    private boolean sendViaIlink(String toUser, String what, IlinkSend call) {
        pace(toUser);
        try {
            JsonObject resp = call.run(contextTokens.getOrDefault(toUser, ""));
            if (accepted(resp)) {
                return true;
            }
            if (isStaleSession(resp)) {
                resp = call.run("");
                if (accepted(resp)) {
                    // 旧 ctx 已死但 tokenless 可发: 摘掉死 ctx, 等下一条入站消息刷新
                    contextTokens.remove(toUser);
                    return true;
                }
                staleSessions.add(toUser);
            }
            System.err.println("[weixin] send " + what + " rejected ret=" + resp.getInteger("ret", 0)
                    + " errcode=" + resp.getInteger("errcode", 0) + " errmsg=" + resp.getString("errmsg", "")
                    + " stale=" + staleSessions.contains(toUser));
            return false;
        } catch (Exception e) {
            System.err.println("[weixin] send " + what + " error: " + e.getMessage());
            return false;
        }
    }

    private static boolean accepted(JsonObject resp) {
        return resp != null && resp.getInteger("ret", 0) == 0 && resp.getInteger("errcode", 0) == 0;
    }

    /** ret=-2 且 errmsg 为空或 unknown error => context_token 失效; 真限流会带 frequency 类文案, 不算失效 */
    private static boolean isStaleSession(JsonObject resp) {
        if (resp == null || resp.getInteger("ret", 0) != RET_STALE_SESSION) {
            return false;
        }
        String msg = orElse(resp.getString("errmsg", ""), "").strip().toLowerCase();
        return msg.isEmpty() || "unknown error".equals(msg);
    }

    /** 同一用户两次发送之间保持最小间隔(发送链串行, 直接 sleep 即可) */
    private void pace(String toUser) {
        long wait = MIN_SEND_INTERVAL_MS - (System.currentTimeMillis() - lastSendAt.getOrDefault(toUser, 0L));
        if (wait > 0) {
            sleep(wait);
        }
        lastSendAt.put(toUser, System.currentTimeMillis());
    }

    private void park(String toUser, Outbound out) {
        Deque<Outbound> q = pending.computeIfAbsent(toUser, k -> new ArrayDeque<>());
        q.addLast(out);
        while (q.size() > MAX_PENDING) {
            System.err.println("[weixin] pending overflow, drop " + q.pollFirst().kind() + " toUser=" + toUser);
        }
        System.out.println("[weixin] park " + out.kind() + " pending=" + q.size() + " toUser=" + toUser);
    }

    /** 拿到新 context_token 后按原顺序补发暂存消息; 若再次失效, submit 会把剩余的按序重新暂存 */
    private void replayPending(String toUser) {
        Deque<Outbound> q = pending.remove(toUser);
        if (q == null || q.isEmpty()) {
            return;
        }
        System.out.println("[weixin] replay pending=" + q.size() + " toUser=" + toUser);
        for (Outbound out : q) {
            submit(toUser, out);
        }
    }

    /** 超长文本切成 ≤ MAX_TEXT_CHARS 的多条: 优先在换行处断, 硬切时避开代理对 */
    private static List<String> splitText(String text) {
        if (text.length() <= MAX_TEXT_CHARS) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + MAX_TEXT_CHARS, text.length());
            if (end < text.length()) {
                int nl = text.lastIndexOf('\n', end - 1);
                if (nl > i) {
                    end = nl + 1;
                } else if (Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
            }
            String part = text.substring(i, end).strip();
            if (!part.isEmpty()) {
                parts.add(part);
            }
            i = end;
        }
        return parts;
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
