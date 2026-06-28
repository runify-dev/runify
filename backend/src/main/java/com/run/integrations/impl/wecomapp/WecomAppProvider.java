package com.run.integrations.impl.wecomapp;

import com.run.dao.entity.Integration;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderInfo;
import com.run.integrations.wecom.WXBizMsgCrypt;
import com.run.integrations.wecom.WecomXml;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信【自建应用】供应商: 验签解密 -> 即时被动回执 -> 主动推送(文本/图片/视频/文件)。
 * 凭证: corpId/agentId/secret/token/aesKey。回复需可信IP。 }
 */
public class WecomAppProvider implements IIntegrationProvider {

    private static final Set<String> PROCESSABLE = Set.of("text", "image", "voice", "video", "file");
    private static final Pattern IMAGE_MD = Pattern.compile("!\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern LINK_MD = Pattern.compile("(?<!!)\\[[^\\]]*]\\(\\s*(\\S+?)\\s*\\)");
    private static final Pattern BARE_URL = Pattern.compile("https?://[^\\s)\\]]+");
    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "webm", "mkv");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "wav", "m4a", "amr", "ogg", "aac");

    @Override
    public IntegrationProviderInfo info() {
        return new IntegrationProviderInfo("WECOM", "企业微信应用");
    }

    private WXBizMsgCrypt crypt(JsonObject config) {
        return new WXBizMsgCrypt(config.getString("token", ""), config.getString("aesKey", ""), config.getString("corpId", ""));
    }

    @Override
    public void verify(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        WXBizMsgCrypt crypt = crypt(config);
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

        String encrypt = WecomXml.extract(context.body().asString(), "Encrypt");
        if (encrypt == null || !crypt.verifySignature(msgSignature, timestamp, nonce, encrypt)) {
            context.response().setStatusCode(401).end();
            return;
        }

        String message = crypt.decrypt(encrypt);
        String msgType = WecomXml.extract(message, "MsgType");
        String fromUser = WecomXml.extract(message, "FromUserName");
        String toUser = WecomXml.extract(message, "ToUserName");

        if (msgType == null || !PROCESSABLE.contains(msgType)) {
            context.response().putHeader("Content-Type", "text/plain").end("success");
            return;
        }

        // 即时被动回执"正在生成中"
        String ackXml = passiveText(fromUser, toUser, "🤔 正在生成中，请稍候…");
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String nc = String.valueOf((long) (Math.random() * 1_000_000_000L));
        String ackEncrypt = crypt.encrypt(ackXml);
        String ackSign = WXBizMsgCrypt.sha1(crypt.getToken(), ts, nc, ackEncrypt);
        context.response().putHeader("Content-Type", "application/xml").end(envelope(ackEncrypt, ackSign, ts, nc));

        WecomAppClient client = new WecomAppClient(deps.vertx());
        buildContent(client, config, msgType, message, deps)
                .compose(contentJson -> deps.dispatcher().dispatch(integration, fromUser, contentJson))
                .compose(reply -> sendReply(client, config, fromUser, reply))
                .onFailure(e -> System.err.println("[wecom-app] dispatch/send failed: " + e.getMessage()));
    }

    // ==================== 入参构建(对齐对话接口 content) ====================

    private Future<JsonObject> buildContent(WecomAppClient client, JsonObject config, String msgType, String message, IntegrationDeps deps) {
        String corpId = config.getString("corpId", "");
        String secret = config.getString("secret", "");
        switch (msgType) {
            case "image":
                return storeMedia(client, corpId, secret, WecomXml.extract(message, "MediaId"), "image.jpg", deps)
                        .map(file -> new JsonObject().put("content", "").put("images", new JsonArray().add(file)));
            case "voice":
                return storeMedia(client, corpId, secret, WecomXml.extract(message, "MediaId"),
                        "voice." + WecomXml.orElse(WecomXml.extract(message, "Format"), "amr"), deps)
                        .map(file -> new JsonObject().put("content", "").put("files", new JsonArray().add(file)));
            case "video":
                return storeMedia(client, corpId, secret, WecomXml.extract(message, "MediaId"), "video.mp4", deps)
                        .map(file -> new JsonObject().put("content", "").put("videos", new JsonArray().add(file)));
            case "file":
                return storeMedia(client, corpId, secret, WecomXml.extract(message, "MediaId"),
                        WecomXml.orElse(WecomXml.extract(message, "FileName"), WecomXml.orElse(WecomXml.extract(message, "Title"), "file")), deps)
                        .map(file -> new JsonObject().put("content", "").put("files", new JsonArray().add(file)));
            default:
                return Future.succeededFuture(new JsonObject().put("content", WecomXml.orElse(WecomXml.extract(message, "Content"), "")));
        }
    }

    private Future<JsonObject> storeMedia(WecomAppClient client, String corpId, String secret, String mediaId, String name, IntegrationDeps deps) {
        if (mediaId == null) {
            return Future.failedFuture("missing MediaId");
        }
        return client.downloadMedia(corpId, secret, mediaId)
                .compose(bytes -> deps.vertx().<java.io.File>executeBlocking(() -> {
                    java.io.File tmp = Files.createTempFile("wecom-", ".bin").toFile();
                    Files.write(tmp.toPath(), bytes);
                    return tmp;
                }, false).compose(tmp -> deps.fileMapper().upload(name, tmp.length(), null, null, tmp)
                        .map(entity -> new JsonObject()
                                .put("url", "./api/storage/file/" + entity.getId())
                                .put("name", name))));
    }

    // ==================== 回复(文本/图片/视频/文件分发) ====================

    private Future<Void> sendReply(WecomAppClient client, JsonObject config, String toUser, String reply) {
        String corpId = config.getString("corpId", "");
        String secret = config.getString("secret", "");
        String agentId = config.getString("agentId", "");

        Set<String> handled = new LinkedHashSet<>();
        List<String[]> media = new ArrayList<>();
        Matcher m = IMAGE_MD.matcher(reply);
        while (m.find()) {
            String u = m.group(1);
            if (handled.add(u)) {
                media.add(new String[]{u, classify(u, "image")});
            }
        }
        for (Pattern p : List.of(LINK_MD, BARE_URL)) {
            Matcher mm = p.matcher(reply);
            while (mm.find()) {
                String u = p == BARE_URL ? mm.group() : mm.group(1);
                String kind = classify(u, "other");
                if (("video".equals(kind) || "audio".equals(kind)) && handled.add(u)) {
                    media.add(new String[]{u, kind});
                }
            }
        }
        String text = IMAGE_MD.matcher(reply).replaceAll("").replaceAll("\\n{3,}", "\n\n").trim();

        Future<Void> chain = Future.succeededFuture();
        if (!text.isEmpty()) {
            chain = chain.compose(v -> client.sendText(corpId, secret, agentId, toUser, text));
        }
        for (String[] mm : media) {
            String url = mm[0];
            String kind = mm[1];
            chain = chain.compose(v -> switch (kind) {
                case "video" -> client.sendVideoByUrl(corpId, secret, agentId, toUser, url);
                case "audio", "file" -> client.sendFileByUrl(corpId, secret, agentId, toUser, url);
                default -> client.sendImageByUrl(corpId, secret, agentId, toUser, url);
            });
        }
        return chain;
    }

    private static String classify(String url, String fallback) {
        String path = url.split("[?#]")[0].toLowerCase();
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return fallback;
        }
        String ext = path.substring(dot + 1);
        if (IMAGE_EXT.contains(ext)) {
            return "image";
        }
        if (VIDEO_EXT.contains(ext)) {
            return "video";
        }
        if (AUDIO_EXT.contains(ext)) {
            return "audio";
        }
        return fallback;
    }

    private String passiveText(String toUserName, String fromUserName, String content) {
        long now = Instant.now().getEpochSecond();
        return "<xml>"
                + "<ToUserName><![CDATA[" + toUserName + "]]></ToUserName>"
                + "<FromUserName><![CDATA[" + fromUserName + "]]></FromUserName>"
                + "<CreateTime>" + now + "</CreateTime>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[" + content + "]]></Content>"
                + "</xml>";
    }

    private String envelope(String encrypt, String signature, String timestamp, String nonce) {
        return "<xml>"
                + "<Encrypt><![CDATA[" + encrypt + "]]></Encrypt>"
                + "<MsgSignature><![CDATA[" + signature + "]]></MsgSignature>"
                + "<TimeStamp>" + timestamp + "</TimeStamp>"
                + "<Nonce><![CDATA[" + nonce + "]]></Nonce>"
                + "</xml>";
    }
}
