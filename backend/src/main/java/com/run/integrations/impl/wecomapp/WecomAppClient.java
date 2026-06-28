package com.run.integrations.impl.wecomapp;

import com.run.common.util.JacksonUtils;
import com.run.integrations.wecom.WecomHttp;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信【自建应用】API 客户端: access_token 缓存 + 应用消息发送 + 素材上传/下载 }
 */
public class WecomAppClient {

    private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

    private record CachedToken(String token, Instant expireAt) {
    }

    private final Vertx vertx;

    public WecomAppClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<String> getAccessToken(String corpId, String secret) {
        CachedToken cached = TOKEN_CACHE.get(corpId);
        if (cached != null && cached.expireAt().isAfter(Instant.now())) {
            return Future.succeededFuture(cached.token());
        }
        return vertx.executeBlocking(() -> {
            String url = WecomHttp.BASE + "/gettoken?corpid=" + WecomHttp.enc(corpId) + "&corpsecret=" + WecomHttp.enc(secret);
            Map<String, Object> resp = WecomHttp.getJson(url);
            Integer errcode = WecomHttp.asInt(resp.get("errcode"));
            if (errcode != null && errcode != 0) {
                throw new RuntimeException("wecom gettoken failed: " + resp.get("errmsg"));
            }
            String token = (String) resp.get("access_token");
            int expiresIn = WecomHttp.asInt(resp.getOrDefault("expires_in", 7200));
            TOKEN_CACHE.put(corpId, new CachedToken(token, Instant.now().plusSeconds(Math.max(60, expiresIn - 200))));
            return token;
        }, false);
    }

    public Future<Void> sendText(String corpId, String secret, String agentId, String toUser, String content) {
        return getAccessToken(corpId, secret).compose(token -> vertx.executeBlocking(() -> {
            Map<String, Object> body = Map.of("touser", toUser, "msgtype", "text",
                    "agentid", parseAgentId(agentId), "text", Map.of("content", content));
            return send(token, body);
        }, false));
    }

    public Future<Void> sendImageByUrl(String corpId, String secret, String agentId, String toUser, String imageUrl) {
        return sendMediaByUrl(corpId, secret, toUser, imageUrl, "image", mediaId ->
                Map.of("touser", toUser, "msgtype", "image", "agentid", parseAgentId(agentId),
                        "image", Map.of("media_id", mediaId)));
    }

    public Future<Void> sendVideoByUrl(String corpId, String secret, String agentId, String toUser, String videoUrl) {
        return sendMediaByUrl(corpId, secret, toUser, videoUrl, "video", mediaId ->
                Map.of("touser", toUser, "msgtype", "video", "agentid", parseAgentId(agentId),
                        "video", Map.of("media_id", mediaId, "title", fileNameFromUrl(videoUrl), "description", "")));
    }

    public Future<Void> sendFileByUrl(String corpId, String secret, String agentId, String toUser, String fileUrl) {
        return sendMediaByUrl(corpId, secret, toUser, fileUrl, "file", mediaId ->
                Map.of("touser", toUser, "msgtype", "file", "agentid", parseAgentId(agentId),
                        "file", Map.of("media_id", mediaId)));
    }

    public Future<byte[]> downloadMedia(String corpId, String secret, String mediaId) {
        return getAccessToken(corpId, secret).compose(token -> vertx.executeBlocking(() -> {
            String url = WecomHttp.BASE + "/media/get?access_token=" + WecomHttp.enc(token) + "&media_id=" + WecomHttp.enc(mediaId);
            return WecomHttp.downloadMedia(url);
        }, false));
    }

    private Future<Void> sendMediaByUrl(String corpId, String secret, String toUser, String mediaUrl,
                                        String mediaType, Function<String, Map<String, Object>> bodyBuilder) {
        return getAccessToken(corpId, secret).compose(token -> vertx.executeBlocking(() -> {
            byte[] data = WecomHttp.downloadBytes(mediaUrl);
            String filename = fileNameFromUrl(mediaUrl);
            String uploadUrl = WecomHttp.BASE + "/media/upload?access_token=" + WecomHttp.enc(token) + "&type=" + mediaType;
            String mediaId = WecomHttp.uploadMultipart(uploadUrl, data, filename, contentTypeFromName(filename));
            return send(token, bodyBuilder.apply(mediaId), mediaType);
        }, false));
    }

    private Void send(String token, Map<String, Object> body) throws Exception {
        return send(token, body, "text");
    }

    private Void send(String token, Map<String, Object> body, String label) throws Exception {
        String url = WecomHttp.BASE + "/message/send?access_token=" + WecomHttp.enc(token);
        Map<String, Object> resp = WecomHttp.postJson(url, JacksonUtils.toJson(body));
        Integer errcode = WecomHttp.asInt(resp.get("errcode"));
        if (errcode != null && errcode != 0) {
            throw new RuntimeException("wecom " + label + " send failed: " + resp.get("errmsg"));
        }
        return null;
    }

    private static Object parseAgentId(String agentId) {
        try {
            return Integer.parseInt(agentId.trim());
        } catch (Exception e) {
            return agentId;
        }
    }

    private static String fileNameFromUrl(String url) {
        String path = url.split("\\?")[0];
        int idx = path.lastIndexOf('/');
        String name = idx >= 0 ? path.substring(idx + 1) : path;
        if (name.isEmpty() || !name.contains(".")) {
            name = "file.bin";
        }
        return name;
    }

    private static String contentTypeFromName(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        return "application/octet-stream";
    }
}
