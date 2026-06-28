package com.run.integrations.impl.wecomstream;

import com.run.dao.entity.Integration;
import com.run.handler.integration.IIntegrationMessageDispatcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    private final Vertx vertx;

    private volatile boolean running = false;
    private volatile WebSocket ws;
    private long heartbeatTimer = -1;
    private long reconnectBackoff = RECONNECT_MIN_MS;

    // JDK WebSocket 要求发送串行: 所有 sendText 串到这条链上, 一次只发一帧
    private final Object sendLock = new Object();
    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);

    public WecomStreamConnection(Integration integration, String botId, String secret,
                                 IIntegrationMessageDispatcher dispatcher, Vertx vertx) {
        this.integration = integration;
        this.botId = botId;
        this.secret = secret;
        this.dispatcher = dispatcher;
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
        String msgType = body.getString("msgtype", "");
        String userId = body.getJsonObject("from", new JsonObject()).getString("userid", "user");
        String content = body.getJsonObject("text", new JsonObject()).getString("content");

        // 首版仅文本; 多媒体(aeskey/AES-256-CBC)留后续
        if (!"text".equals(msgType) || content == null) {
            respondStream(reqId, msgid, "🤔 暂只支持文本消息", true);
            return;
        }

        String question = stripMention(content);
        // 首帧: 先占位, 表示已收到正在处理
        respondStream(reqId, msgid, "🤔 正在思考…", false);
        dispatcher.dispatchStream(integration, userId, new JsonObject().put("content", question),
                        snapshot -> respondStream(reqId, msgid, snapshot, false))
                .onSuccess(reply -> respondStream(reqId, msgid,
                        (reply == null || reply.isBlank()) ? "(无回复)" : reply, true))
                .onFailure(e -> {
                    System.err.println("[wecom-stream] dispatch failed: " + e.getMessage());
                    respondStream(reqId, msgid, "⚠️ 处理失败: " + e.getMessage(), true);
                });
    }

    /**
     * 主动下发一帧流式回复; finish=true 为结束帧。同一条消息全程复用 streamId。
     */
    private void respondStream(String reqId, String streamId, String content, boolean finish) {
        String text = (content == null || content.isEmpty()) ? "🤔 正在思考…" : content;
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

    private static String reqId() {
        return UUID.randomUUID().toString();
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
