package com.run.integrations.impl.feishu;

import com.run.common.util.JacksonUtils;
import com.run.integrations.Http;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 飞书 API 客户端: tenant_access_token 缓存 + 发送消息 }
 */
public class FeishuClient {

    private static final String BASE = "https://open.feishu.cn/open-apis";
    private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

    private record CachedToken(String token, Instant expireAt) {
    }

    private final Vertx vertx;

    public FeishuClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<String> tenantToken(String appId, String appSecret) {
        CachedToken cached = TOKEN_CACHE.get(appId);
        if (cached != null && cached.expireAt().isAfter(Instant.now())) {
            return Future.succeededFuture(cached.token());
        }
        return vertx.executeBlocking(() -> {
            Map<String, Object> resp = Http.postJson(BASE + "/auth/v3/tenant_access_token/internal",
                    JacksonUtils.toJson(Map.of("app_id", appId, "app_secret", appSecret)), null);
            Integer code = Http.asInt(resp.get("code"));
            if (code != null && code != 0) {
                throw new RuntimeException("feishu token failed: " + resp.get("msg"));
            }
            String token = (String) resp.get("tenant_access_token");
            int expire = Http.asInt(resp.getOrDefault("expire", 7200));
            TOKEN_CACHE.put(appId, new CachedToken(token, Instant.now().plusSeconds(Math.max(60, expire - 200))));
            return token;
        }, false);
    }

    /**
     * 向会话发送文本消息(receive_id_type=chat_id)
     */
    public Future<Void> sendText(String appId, String appSecret, String chatId, String text) {
        return tenantToken(appId, appSecret).compose(token -> vertx.executeBlocking(() -> {
            String url = BASE + "/im/v1/messages?receive_id_type=chat_id";
            Map<String, Object> body = Map.of(
                    "receive_id", chatId,
                    "msg_type", "text",
                    "content", new JsonObject().put("text", text).encode());
            Map<String, Object> resp = Http.postJson(url, JacksonUtils.toJson(body), token);
            Integer code = Http.asInt(resp.get("code"));
            if (code != null && code != 0) {
                throw new RuntimeException("feishu send failed: " + resp.get("msg"));
            }
            return null;
        }, false));
    }
}
