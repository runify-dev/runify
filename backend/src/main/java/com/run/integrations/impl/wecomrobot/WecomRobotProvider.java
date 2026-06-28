package com.run.integrations.impl.wecomrobot;

import com.run.dao.entity.Integration;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderInfo;
import com.run.integrations.wecom.WXBizMsgCrypt;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信【智能机器人】供应商, 流式回复(被动回复)。
 * 回调为 JSON 信封 {"encrypt":"..."}, 解密后内层 JSON。流式: 在回调的 HTTP 响应体里返回加密的 stream 帧,
 * finish=false 时企业微信会再次回调取下一帧, 直到 finish=true(最多 6 分钟)。凭证只需 token/aesKey。 }
 */
public class WecomRobotProvider implements IIntegrationProvider {

    private static final Map<String, StreamSession> SESSIONS = new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MS = 10 * 60 * 1000L;

    @Override
    public IntegrationProviderInfo info() {
        return new IntegrationProviderInfo("WECOM_ROBOT", "企业微信机器人");
    }

    private WXBizMsgCrypt crypt(JsonObject config) {
        return new WXBizMsgCrypt(config.getString("token", ""), config.getString("aesKey", ""), config.getString("corpId", ""));
    }

    @Override
    public void verify(RoutingContext context, Integration integration, IntegrationDeps deps) {
        WXBizMsgCrypt crypt = crypt(integration.decrypt());
        String msgSignature = context.request().getParam("msg_signature");
        String timestamp = context.request().getParam("timestamp");
        String nonce = context.request().getParam("nonce");
        String echostr = context.request().getParam("echostr");
        if (!crypt.verifySignature(msgSignature, timestamp, nonce, echostr)) {
            context.response().setStatusCode(401).end();
            return;
        }
        context.response().putHeader("Content-Type", "text/plain").end(crypt.decrypt(echostr));
    }

    @Override
    public void handleMessage(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        WXBizMsgCrypt crypt = crypt(config);
        String msgSignature = context.request().getParam("msg_signature");
        String timestamp = context.request().getParam("timestamp");
        String nonce = context.request().getParam("nonce");

        String encrypt;
        try {
            encrypt = new JsonObject(context.body().asString()).getString("encrypt");
        } catch (Exception e) {
            encrypt = null;
        }
        if (encrypt == null || !crypt.verifySignature(msgSignature, timestamp, nonce, encrypt)) {
            context.response().setStatusCode(encrypt == null ? 400 : 401).end();
            return;
        }

        String decrypted = crypt.decrypt(encrypt);
        String receiveId = crypt.getLastReceiveId();

        JsonObject msg;
        try {
            msg = new JsonObject(decrypted);
        } catch (Exception e) {
            System.err.println("[wecom-robot] decrypted not json: " + decrypted);
            context.response().end("");
            return;
        }

        sweepStale();
        String token = config.getString("token", "");
        String aesKey = config.getString("aesKey", "");
        // 用入站 receiveId 回加密
        WXBizMsgCrypt replyCrypt = new WXBizMsgCrypt(token, aesKey, receiveId == null ? "" : receiveId);

        String msgid = msg.getString("msgid");
        String streamId = msg.getJsonObject("stream", new JsonObject()).getString("id", msgid);
        StreamSession existing = streamId != null ? SESSIONS.get(streamId) : null;

        if (existing != null) {
            boolean finished = existing.isFinished();
            writeStreamFrame(context, replyCrypt, token, streamId, existing.content(), finished);
            if (finished) {
                SESSIONS.remove(streamId);
            }
            return;
        }

        String msgType = msg.getString("msgtype");
        String content = msg.getJsonObject("text", new JsonObject()).getString("content");
        String userId = msg.getJsonObject("from", new JsonObject()).getString("userid", "robot");

        if (!"text".equals(msgType) || content == null) {
            // 非文本且无匹配会话: 很可能是没关联上的刷新帧, 不发 finish=true 以免误结束
            writeStreamFrame(context, replyCrypt, token, streamId, "🤔 正在思考…", false);
            return;
        }

        StreamSession session = new StreamSession();
        SESSIONS.put(streamId, session);
        String question = stripMention(content);
        deps.dispatcher()
                .dispatchStream(integration, userId, new JsonObject().put("content", question), session::set)
                .onSuccess(session::complete)
                .onFailure(e -> {
                    System.err.println("[wecom-robot] dispatch failed: " + e.getMessage());
                    session.fail(e.getMessage());
                });

        // 首帧
        writeStreamFrame(context, replyCrypt, token, streamId, session.content(), session.isFinished());
    }

    /**
     * 把一帧 stream 加密后写入回调 HTTP 响应体(被动回复)。视频转成可点击 markdown 链接。
     */
    private void writeStreamFrame(RoutingContext context, WXBizMsgCrypt replyCrypt, String token,
                                  String streamId, String rawContent, boolean finish) {
        String raw = rawContent == null ? "" : rawContent;
        // 结束帧: 去掉残留的"进行中"工具块(工具调用只在调用过程中显示)
        if (finish) {
            raw = stripToolBlock(raw);
        }
        String text = raw.isEmpty() ? "🤔 正在思考…" : videoToLink(raw);

        JsonObject inner = new JsonObject()
                .put("msgtype", "stream")
                .put("stream", new JsonObject().put("id", streamId).put("finish", finish).put("content", text));

        String ts = String.valueOf(Instant.now().getEpochSecond());
        String nc = String.valueOf((long) (Math.random() * 1_000_000_000L));
        String enc = replyCrypt.encrypt(inner.encode());
        String sig = WXBizMsgCrypt.sha1(token, ts, nc, enc);
        JsonObject envelope = new JsonObject()
                .put("encrypt", enc)
                .put("msgsignature", sig)
                .put("timestamp", ts)
                .put("nonce", nc);
        context.response().putHeader("Content-Type", "application/json").end(envelope.encode());
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

    /**
     * 结束帧用卡片承载视频时, 正文里去掉视频标记
     */
    private static String stripVideo(String content) {
        return content
                .replaceAll("(?s)<video[^>]*>.*?</video>", "")
                .replaceAll("(?s)<video[^>]*/?>", "")
                .replaceAll("!\\[[^\\]]*]\\([^)]+\\.(?:mp4|mov|webm|mkv)[^)]*\\)", "")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    /**
     * 群里 @机器人 时 content 带"@机器人名"提及。企业微信在 @提及后跟特殊分隔符  ,
     * 故优先去掉「开头@ 到第一个  」整段(机器人名可含空格); 无分隔符时退化按 @\S+ + 空白处理。
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

    private static void sweepStale() {
        long now = System.currentTimeMillis();
        SESSIONS.entrySet().removeIf(e -> now - e.getValue().createdAt > SESSION_TTL_MS);
    }
}
