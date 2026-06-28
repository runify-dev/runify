package com.run.integrations.impl.wechat;

import com.run.common.util.JacksonUtils;
import com.run.integrations.Http;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 微信公众号客户端: access_token 缓存 + 客服消息主动下发(需认证服务号, 用户 48 小时内有互动) }
 */
public class WechatClient {

    private static final String BASE = "https://api.weixin.qq.com/cgi-bin";
    private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

    private record CachedToken(String token, Instant expireAt) {
    }

    private final Vertx vertx;

    public WechatClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<String> getAccessToken(String appId, String appSecret) {
        CachedToken cached = TOKEN_CACHE.get(appId);
        if (cached != null && cached.expireAt().isAfter(Instant.now())) {
            return Future.succeededFuture(cached.token());
        }
        return vertx.executeBlocking(() -> {
            String url = BASE + "/token?grant_type=client_credential&appid=" + enc(appId) + "&secret=" + enc(appSecret);
            Map<String, Object> resp = Http.getJson(url, null);
            Integer errcode = Http.asInt(resp.get("errcode"));
            if (errcode != null && errcode != 0) {
                throw new RuntimeException("wechat token failed: " + resp.get("errmsg"));
            }
            String token = (String) resp.get("access_token");
            int expiresIn = Http.asInt(resp.getOrDefault("expires_in", 7200));
            TOKEN_CACHE.put(appId, new CachedToken(token, Instant.now().plusSeconds(Math.max(60, expiresIn - 200))));
            return token;
        }, false);
    }

    public Future<Void> sendCustomText(String appId, String appSecret, String openid, String content) {
        return getAccessToken(appId, appSecret).compose(token -> vertx.executeBlocking(() -> {
            String url = BASE + "/message/custom/send?access_token=" + enc(token);
            Map<String, Object> body = Map.of("touser", openid, "msgtype", "text",
                    "text", Map.of("content", content));
            Map<String, Object> resp = Http.postJson(url, JacksonUtils.toJson(body), null);
            Integer errcode = Http.asInt(resp.get("errcode"));
            if (errcode != null && errcode != 0) {
                throw new RuntimeException("wechat custom/send failed: " + resp.get("errmsg"));
            }
            return null;
        }, false));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
