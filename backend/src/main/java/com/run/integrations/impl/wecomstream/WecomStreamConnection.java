package com.run.integrations.impl.wecomstream;

import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.entity.Integration;
import com.run.dao.mapper.FileMapper;
import com.run.handler.integration.IIntegrationMessageDispatcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/28  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信【智能机器人】长连接(WebSocket)单连接: bot_id/secret 订阅(aibot_subscribe) + 30s 心跳(ping) +
 * 收消息(aibot_msg_callback) -> 跑应用 -> 主动 push 流式帧(aibot_respond_msg, finish=false..true)。
 * <p>
 * 用 JDK java.net.http.WebSocket(与 weixin 的 IlinkClient 一致), 不用 Vert.x WS:
 * 腾讯 openws 网关对升级请求的头名大小写敏感, 而 Vert.x 5 会把 HTTP/1.1 握手头名写成小写, 导致握手被判 404。
 * JDK 客户端发标准大小写头, 握手正常。注意 JDK WebSocket 要求发送串行(上一个 send 完成前不能再发), 故所有发送走 sendChain 串联。 }
 */
public class WecomStreamConnection {

    private static final URI WS_URI = URI.create("wss://openws.work.weixin.qq.com");
    private static final long HEARTBEAT_MS = 30_000L;
    private static final long RECONNECT_MIN_MS = 2_000L;
    private static final long RECONNECT_MAX_MS = 60_000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private final Integration integration;
    private final String botId;
    private final String secret;
    private final IIntegrationMessageDispatcher dispatcher;
    private final FileMapper fileMapper;
    private final Vertx vertx;

    private volatile boolean running = false;
    private volatile WebSocket ws;
    private long heartbeatTimer = -1;
    private long reconnectBackoff = RECONNECT_MIN_MS;

    // JDK WebSocket 要求发送串行: 所有 sendText 串到这条链上, 一次只发一帧
    private final Object sendLock = new Object();
    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);

    // 媒体上传走请求/响应: 发出去的帧按 req_id 等回包(init/chunk/finish), 回包到达时由 onFrame 完成对应 future
    private final Map<String, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private static final int CHUNK_SIZE = 512 * 1024;

    public WecomStreamConnection(Integration integration, String botId, String secret,
                                 IIntegrationMessageDispatcher dispatcher, FileMapper fileMapper, Vertx vertx) {
        this.integration = integration;
        this.botId = botId;
        this.secret = secret;
        this.dispatcher = dispatcher;
        this.fileMapper = fileMapper;
        this.vertx = vertx;
    }

    public void start() {
        running = true;
        connect();
    }

    public void stop() {
        running = false;
        cancelHeartbeat();
        WebSocket w = ws;
        ws = null;
        if (w != null) {
            try {
                w.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 连接 / 重连 ====================

    private void connect() {
        if (!running) {
            return;
        }
        HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(WS_URI, new Listener())
                .whenComplete((socket, err) -> {
                    if (err != null) {
                        System.err.println("[wecom-stream] connect failed: " + rootMsg(err));
                        scheduleReconnect();
                        return;
                    }
                    this.ws = socket;
                    synchronized (sendLock) {
                        sendChain = CompletableFuture.completedFuture(null);
                    }
                    subscribe();
                    startHeartbeat();
                    System.out.println("[wecom-stream] connected, bot=" + botId);
                });
    }

    private void scheduleReconnect() {
        if (!running) {
            return;
        }
        long delay = reconnectBackoff;
        reconnectBackoff = Math.min(RECONNECT_MAX_MS, reconnectBackoff * 2);
        vertx.setTimer(delay, id -> connect());
    }

    /**
     * JDK WebSocket 监听: onText 可能分片(last=false), 按消息缓冲到完整再解析
     */
    private final class Listener implements WebSocket.Listener {
        private final StringBuilder buf = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String msg = buf.toString();
                buf.setLength(0);
                try {
                    onFrame(msg);
                } catch (Exception e) {
                    System.err.println("[wecom-stream] handle frame error: " + e.getMessage());
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.err.println("[wecom-stream] ws closed code=" + statusCode + " reason=" + reason + " bot=" + botId);
            cancelHeartbeat();
            ws = null;
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("[wecom-stream] ws error: " + rootMsg(error));
            cancelHeartbeat();
            ws = null;
            scheduleReconnect();
        }
    }

    // ==================== 帧收发 ====================

    private void subscribe() {
        JsonObject frame = new JsonObject()
                .put("cmd", "aibot_subscribe")
                .put("headers", new JsonObject().put("req_id", reqId()))
                .put("body", new JsonObject().put("bot_id", botId).put("secret", secret));
        send(frame.encode());
    }

    private void startHeartbeat() {
        cancelHeartbeat();
        heartbeatTimer = vertx.setPeriodic(HEARTBEAT_MS, id -> {
            JsonObject ping = new JsonObject()
                    .put("cmd", "ping")
                    .put("headers", new JsonObject().put("req_id", reqId()));
            send(ping.encode());
        });
    }

    private void cancelHeartbeat() {
        if (heartbeatTimer >= 0) {
            vertx.cancelTimer(heartbeatTimer);
            heartbeatTimer = -1;
        }
    }

    private void onFrame(String text) {
        JsonObject frame = new JsonObject(text);
        // 媒体上传等请求的回包: 按 req_id 命中 pending 则完成对应 future(无论是否带 cmd)
        String rid = frame.getJsonObject("headers", new JsonObject()).getString("req_id", "");
        if (!rid.isEmpty()) {
            CompletableFuture<JsonObject> p = pending.remove(rid);
            if (p != null) {
                int errcode = frame.getInteger("errcode", 0);
                if (errcode != 0) {
                    p.completeExceptionally(new RuntimeException("errcode " + errcode + " " + frame.getString("errmsg", "")));
                } else {
                    p.complete(frame.getJsonObject("body", new JsonObject()));
                }
                return;
            }
        }
        String cmd = frame.getString("cmd", "");
        if (cmd.isEmpty()) {
            // 认证/心跳响应: {headers, errcode, errmsg}
            int errcode = frame.getInteger("errcode", 0);
            if (errcode == 0) {
                reconnectBackoff = RECONNECT_MIN_MS;
            } else {
                System.err.println("[wecom-stream] ack errcode=" + errcode + " errmsg=" + frame.getString("errmsg", ""));
            }
            return;
        }
        switch (cmd) {
            case "aibot_msg_callback" -> handleMsgCallback(frame);
            case "aibot_event_callback" -> System.out.println("[wecom-stream] event: " + frame.encode());
            default -> System.out.println("[wecom-stream] frame cmd=" + cmd);
        }
    }

    private void handleMsgCallback(JsonObject frame) {
        String reqId = frame.getJsonObject("headers", new JsonObject()).getString("req_id", reqId());
        JsonObject body = frame.getJsonObject("body", new JsonObject());
        String msgid = body.getString("msgid", UUID.randomUUID().toString());
        String userId = body.getJsonObject("from", new JsonObject()).getString("userid", "user");

        // 首帧: 先占位, 表示已收到正在处理
        respondStream(reqId, msgid, "🤔 正在思考…", false);
        // 媒体需下载+解密(阻塞), 丢到虚拟线程, 避免卡住 WS 读线程(影响后续收帧/心跳)
        Thread.ofVirtual().start(() -> {
            JsonObject content;
            try {
                content = buildContent(body);
            } catch (Exception e) {
                System.err.println("[wecom-stream] build content failed: " + e.getMessage());
                respondStream(reqId, msgid, "⚠️ 消息解析失败: " + e.getMessage(), true);
                return;
            }
            if (content == null) {
                respondStream(reqId, msgid, "🤔 暂不支持该消息类型", true);
                return;
            }
            dispatcher.dispatchStream(integration, userId, content,
                            snapshot -> respondStream(reqId, msgid, snapshot, false))
                    .onSuccess(reply -> Thread.ofVirtual().start(() -> deliverReply(reqId, msgid, reply)))
                    .onFailure(e -> {
                        System.err.println("[wecom-stream] dispatch failed: " + e.getMessage());
                        respondStream(reqId, msgid, "⚠️ 处理失败: " + e.getMessage(), true);
                    });
        });
    }

    /**
     * 下发最终回复: 先把媒体(内部存储文件/绝对URL图片视频文件)从正文剥离, 发剥离后的文本(finish=true),
     * 再对每个媒体走"上传->media_id->原生媒体帧"追发。媒体上传是阻塞的, 本方法已在虚拟线程内执行。
     */
    private void deliverReply(String reqId, String msgid, String reply) {
        String full = (reply == null || reply.isBlank()) ? "(无回复)" : reply;
        WecomStreamOutbound.Result o = WecomStreamOutbound.collect(full);
        boolean hasMedia = !o.storageIds().isEmpty() || !o.images().isEmpty() || !o.videos().isEmpty() || !o.files().isEmpty();
        String caption = o.text().isBlank() ? (hasMedia ? "📎 已为你生成内容" : "(无回复)") : o.text();
        respondStream(reqId, msgid, caption, true);

        for (String id : o.storageIds()) {
            sendStorageMedia(reqId, id);
        }
        for (String url : o.images()) {
            sendUrlMedia(reqId, url, "image");
        }
        for (String url : o.videos()) {
            sendUrlMedia(reqId, url, "video");
        }
        for (String url : o.files()) {
            sendUrlMedia(reqId, url, "file");
        }
    }

    /**
     * 入站消息 -> 应用入参 {content, images[], videos[], files[]}(与对话接口同构)。
     * text/voice(已转写) 走文本; image/file/video 下载解密后存内部存储; mixed 按子项拆分。
     */
    private JsonObject buildContent(JsonObject body) throws Exception {
        String msgType = body.getString("msgtype", "");
        StringBuilder text = new StringBuilder();
        JsonArray images = new JsonArray();
        JsonArray videos = new JsonArray();
        JsonArray files = new JsonArray();
        switch (msgType) {
            case "text" -> text.append(body.getJsonObject("text", new JsonObject()).getString("content", ""));
            case "voice" -> text.append(body.getJsonObject("voice", new JsonObject()).getString("content", ""));
            case "image" -> images.add(storeInbound(body.getJsonObject("image"), "image.jpg"));
            case "video" -> videos.add(storeInbound(body.getJsonObject("video"), "video.mp4"));
            case "file" -> files.add(storeInbound(body.getJsonObject("file"), "file.bin"));
            case "mixed" -> {
                JsonArray items = body.getJsonObject("mixed", new JsonObject()).getJsonArray("msg_item", new JsonArray());
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.getJsonObject(i);
                    if ("text".equals(item.getString("msgtype"))) {
                        text.append(item.getJsonObject("text", new JsonObject()).getString("content", ""));
                    } else if ("image".equals(item.getString("msgtype"))) {
                        images.add(storeInbound(item.getJsonObject("image"), "image.jpg"));
                    }
                }
            }
            default -> {
                return null;
            }
        }
        String question = stripMention(text.toString());
        if (question.isEmpty() && images.isEmpty() && videos.isEmpty() && files.isEmpty()) {
            return null;
        }
        JsonObject content = new JsonObject().put("content", question);
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

    /**
     * 下载解密一份入站媒体并存入内部存储, 返回 {url: ./api/storage/file/{id}, name}
     */
    private JsonObject storeInbound(JsonObject media, String fallbackName) throws Exception {
        WecomStreamMedia.Media m = WecomStreamMedia.fetch(media.getString("url"), media.getString("aeskey"));
        String name = (m.filename() == null || m.filename().isEmpty()) ? fallbackName : m.filename();
        java.io.File tmp = Files.createTempFile("wecom-", ".bin").toFile();
        Files.write(tmp.toPath(), m.data());
        var entity = fileMapper.upload(name, tmp.length(), null, null, tmp)
                .toCompletionStage().toCompletableFuture().get();
        return new JsonObject().put("url", "./api/storage/file/" + entity.getId()).put("name", name);
    }

    /**
     * 主动下发一帧流式回复; finish=true 为结束帧。同一条消息全程复用 streamId。
     * 内容是 markdown: 结束帧剥掉残留的工具块; &lt;video&gt;/![](*.mp4) 转可点击链接(markdown 不渲染视频)。
     */
    private void respondStream(String reqId, String streamId, String content, boolean finish) {
        String raw = content == null ? "" : content;
        if (finish) {
            raw = stripToolBlock(raw);
        }
        String text = raw.isEmpty() ? "🤔 正在思考…" : videoToLink(raw);
        JsonObject frame = new JsonObject()
                .put("cmd", "aibot_respond_msg")
                .put("headers", new JsonObject().put("req_id", reqId))
                .put("body", new JsonObject()
                        .put("msgtype", "stream")
                        .put("stream", new JsonObject()
                                .put("id", streamId)
                                .put("finish", finish)
                                .put("content", text)));
        send(frame.encode());
    }

    /**
     * 串行发送: JDK WebSocket 要求上一个 sendText 完成前不能再发, 故所有帧串到 sendChain 上排队。
     */
    private void send(String frame) {
        synchronized (sendLock) {
            sendChain = sendChain
                    .thenCompose(ignored -> {
                        WebSocket w = ws;
                        return w == null ? CompletableFuture.completedFuture(null) : w.sendText(frame, true);
                    })
                    .exceptionally(e -> {
                        System.err.println("[wecom-stream] send failed: " + rootMsg(e));
                        return null;
                    });
        }
    }

    // ==================== 出站原生媒体: 上传(分片) -> media_id -> 媒体帧 ====================

    /**
     * 内部存储文件: 直接从 fileMapper 读字节(不走 HTTP, 不需公网), 按文件名后缀判类型发原生媒体
     */
    private void sendStorageMedia(String reqId, String fileId) {
        try {
            FileEntity fe = fileMapper.getById(fileId).toCompletionStage().toCompletableFuture().get();
            if (fe == null) {
                return;
            }
            String name = orElse(fe.getFileName(), "file.bin");
            byte[] data = readStorageBytes(fe);
            String type = orElse(WecomStreamOutbound.classify(name), "file");
            String mediaId = uploadMedia(data, name, type);
            sendMediaFrame(reqId, type, mediaId);
        } catch (Exception e) {
            System.err.println("[wecom-stream] send storage media failed: " + rootMsg(e));
        }
    }

    /**
     * 绝对 URL 媒体: 下载后按 kind(image/video/file) 上传发原生媒体
     */
    private void sendUrlMedia(String reqId, String url, String kind) {
        try {
            WecomStreamMedia.Media m = WecomStreamMedia.fetch(url, null);
            String name = (m.filename() == null || m.filename().isEmpty())
                    ? WecomStreamOutbound.fileNameFromUrl(url, kind) : m.filename();
            String mediaId = uploadMedia(m.data(), name, kind);
            sendMediaFrame(reqId, kind, mediaId);
        } catch (Exception e) {
            System.err.println("[wecom-stream] send url media failed: " + rootMsg(e));
        }
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

    /**
     * 分片上传临时素材: init(拿 upload_id) -> 逐块 base64(512KB) -> finish(拿 media_id)
     */
    private String uploadMedia(byte[] data, String filename, String type) throws Exception {
        int total = Math.max(1, (int) Math.ceil(data.length / (double) CHUNK_SIZE));
        JsonObject init = sendRequest("aibot_upload_media_init", new JsonObject()
                .put("type", type).put("filename", filename)
                .put("total_size", data.length).put("total_chunks", total).put("md5", md5Hex(data)), 15_000);
        String uploadId = init.getString("upload_id");
        for (int i = 0; i < total; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, data.length);
            String b64 = Base64.getEncoder().encodeToString(Arrays.copyOfRange(data, start, end));
            sendRequest("aibot_upload_media_chunk", new JsonObject()
                    .put("upload_id", uploadId).put("chunk_index", i).put("base64_data", b64), 30_000);
        }
        JsonObject fin = sendRequest("aibot_upload_media_finish", new JsonObject().put("upload_id", uploadId), 30_000);
        return fin.getString("media_id");
    }

    private void sendMediaFrame(String reqId, String type, String mediaId) {
        JsonObject frame = new JsonObject()
                .put("cmd", "aibot_respond_msg")
                .put("headers", new JsonObject().put("req_id", reqId))
                .put("body", new JsonObject()
                        .put("msgtype", type)
                        .put(type, new JsonObject().put("media_id", mediaId)));
        send(frame.encode());
    }

    /**
     * 发一帧请求并阻塞等回包(按 req_id 关联); 超时则失败。仅供虚拟线程调用。
     */
    private JsonObject sendRequest(String cmd, JsonObject body, long timeoutMs) throws Exception {
        String rid = reqId();
        CompletableFuture<JsonObject> fut = new CompletableFuture<>();
        pending.put(rid, fut);
        long timer = vertx.setTimer(timeoutMs, t -> {
            CompletableFuture<JsonObject> f = pending.remove(rid);
            if (f != null) {
                f.completeExceptionally(new RuntimeException("request timeout cmd=" + cmd));
            }
        });
        send(new JsonObject().put("cmd", cmd).put("headers", new JsonObject().put("req_id", rid)).put("body", body).encode());
        try {
            return fut.get();
        } finally {
            vertx.cancelTimer(timer);
            pending.remove(rid);
        }
    }

    private static String md5Hex(byte[] data) throws Exception {
        byte[] d = MessageDigest.getInstance("MD5").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String orElse(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static String reqId() {
        return UUID.randomUUID().toString();
    }

    /**
     * markdown 无法渲染/播放视频, 把 &lt;video src&gt; 和 ![](*.mp4) 转成可点击链接
     */
    private static String videoToLink(String content) {
        return content
                .replaceAll("(?s)<video[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*>.*?</video>", "[▶️ 查看视频]($1)")
                .replaceAll("(?s)<video[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*/?>", "[▶️ 查看视频]($1)")
                .replaceAll("!\\[[^\\]]*]\\(([^)]+\\.(?:mp4|mov|webm|mkv)[^)]*)\\)", "[▶️ 查看视频]($1)");
    }

    /**
     * 工具块由 renderFull 追加在末尾(\n🔧 **名称** ⏳…), 结束帧整体剥掉
     */
    private static String stripToolBlock(String content) {
        int idx = content.lastIndexOf("\n🔧 ");
        return idx >= 0 ? content.substring(0, idx).stripTrailing() : content;
    }

    private static String rootMsg(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    /**
     * 群里 @机器人 时正文带"@机器人名"提及, 企业微信用特殊分隔符( / ); 去掉开头整段提及
     */
    private static String stripMention(String content) {
        if (content == null) {
            return null;
        }
        String s = content.replaceFirst("^\\s*@[^\\u2005\\n]*[\\u2005\\u00a0]+", "");
        if (s.equals(content)) {
            s = content.replaceFirst("^\\s*@\\S+\\s+", "");
        }
        return s.strip();
    }
}
