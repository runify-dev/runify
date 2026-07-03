package com.run.integrations.impl.feishu;

import com.run.dao.entity.Integration;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderInfo;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 飞书供应商: 事件订阅(POST)。url_verification 握手 + im.message.receive_v1 消息;
 * 回复通过 tenant_access_token 调 API 主动发送。凭证: appId/appSecret/encryptKey(可选)/verifyToken(可选)。 }
 */
public class FeishuProvider implements IIntegrationProvider {

    // 飞书事件是至少一次投递(响应慢会重试), 按 event_id + 5min 窗口去重, 避免重复触发工作流
    private static final long DEDUP_TTL_MS = 5 * 60 * 1000L;
    private final Map<String, Long> seenEvents = new ConcurrentHashMap<>();

    @Override
    public IntegrationProviderInfo info() {
        return new IntegrationProviderInfo("FEISHU", "飞书");
    }

    @Override
    public void verify(RoutingContext context, Integration integration, IntegrationDeps deps) {
        // 飞书无 GET 验证, challenge 走 POST
        context.response().putHeader("Content-Type", "application/json").end("{}");
    }

    @Override
    public void handleMessage(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        String encryptKey = config.getString("encryptKey", "");

        JsonObject root;
        try {
            root = new JsonObject(context.body().asString());
            if (root.containsKey("encrypt")) {
                root = new JsonObject(new FeishuCrypt(encryptKey).decrypt(root.getString("encrypt")));
            }
        } catch (Exception e) {
            System.err.println("[feishu] decrypt/parse failed: " + e.getMessage());
            context.response().setStatusCode(400).end();
            return;
        }

        // 校验 Verification Token(配置了才校验): url_verification 带在根节点, v2 事件带在 header 里。
        // 不校验的话, 未配置 encryptKey 时回调 URL 泄露即可伪造事件白嫖工作流
        JsonObject header = root.getJsonObject("header", new JsonObject());
        String verifyToken = config.getString("verifyToken", "");
        if (!verifyToken.isEmpty()) {
            String token = root.getString("token", header.getString("token", ""));
            if (!verifyToken.equals(token)) {
                System.err.println("[feishu] verification token mismatch");
                context.response().setStatusCode(401).end();
                return;
            }
        }

        // URL 验证握手
        if ("url_verification".equals(root.getString("type"))) {
            context.response().putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("challenge", root.getString("challenge")).encode());
            return;
        }

        // 事件: 立即回 200(重复事件也回 200, 让飞书停止重试)
        context.response().putHeader("Content-Type", "application/json").end("{}");

        if (!"im.message.receive_v1".equals(header.getString("event_type"))) {
            return;
        }
        String eventId = header.getString("event_id", "");
        if (!eventId.isEmpty() && isDuplicate(eventId)) {
            return;
        }

        JsonObject event = root.getJsonObject("event", new JsonObject());
        JsonObject message = event.getJsonObject("message", new JsonObject());
        if (!"text".equals(message.getString("message_type"))) {
            return;
        }
        String chatId = message.getString("chat_id");
        String openId = event.getJsonObject("sender", new JsonObject())
                .getJsonObject("sender_id", new JsonObject()).getString("open_id", "feishu");

        String text;
        try {
            text = new JsonObject(message.getString("content", "{}")).getString("text", "");
        } catch (Exception e) {
            text = "";
        }
        if (text.isEmpty()) {
            return;
        }

        String appId = config.getString("appId", "");
        String appSecret = config.getString("appSecret", "");
        FeishuClient client = new FeishuClient(deps.vertx());
        deps.dispatcher()
                .dispatch(integration, openId, new JsonObject().put("content", stripMention(text)))
                .compose(reply -> client.sendText(appId, appSecret, chatId, (reply == null || reply.isBlank()) ? "(无回复)" : reply))
                .onFailure(e -> System.err.println("[feishu] dispatch/send failed: " + e.getMessage()));
    }

    /**
     * 群里 @机器人 时 content 文本含 @_user_x / @_all 占位, 去掉
     */
    private static String stripMention(String text) {
        return text.replaceAll("@_user_\\d+", "").replaceAll("@_all", "").strip();
    }

    private boolean isDuplicate(String eventId) {
        long now = System.currentTimeMillis();
        seenEvents.entrySet().removeIf(e -> now - e.getValue() > DEDUP_TTL_MS);
        return seenEvents.putIfAbsent(eventId, now) != null;
    }
}
