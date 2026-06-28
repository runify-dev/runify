package com.run.integrations.impl.dingtalk;

import com.run.common.util.JacksonUtils;
import com.run.integrations.Http;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 钉钉客户端: 通过消息里带回的 sessionWebhook 回复(无需 token, markdown 支持图片) }
 */
public class DingTalkClient {

    private final Vertx vertx;

    public DingTalkClient(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<Void> sendMarkdown(String sessionWebhook, String title, String text) {
        return vertx.executeBlocking(() -> {
            Map<String, Object> body = Map.of("msgtype", "markdown",
                    "markdown", Map.of("title", title, "text", text));
            Map<String, Object> resp = Http.postJson(sessionWebhook, JacksonUtils.toJson(body), null);
            Integer errcode = Http.asInt(resp.get("errcode"));
            if (errcode != null && errcode != 0) {
                throw new RuntimeException("dingtalk send failed: " + resp.get("errmsg"));
            }
            return null;
        }, false);
    }
}
