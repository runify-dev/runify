package com.run.integrations.impl.dingtalk;

import com.run.dao.entity.Integration;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderInfo;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 钉钉企业内部机器人(outgoing 回调): HmacSHA256 验签 -> 解析 -> 通过 sessionWebhook 回 markdown。
 * 凭证: appSecret(验签用)。 }
 */
public class DingTalkProvider implements IIntegrationProvider {

    @Override
    public IntegrationProviderInfo info() {
        return new IntegrationProviderInfo("DINGTALK", "钉钉");
    }

    @Override
    public void verify(RoutingContext context, Integration integration, IntegrationDeps deps) {
        context.response().putHeader("Content-Type", "application/json").end("{}");
    }

    @Override
    public void handleMessage(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        String appSecret = config.getString("appSecret", "");

        String timestamp = context.request().getHeader("timestamp");
        String sign = context.request().getHeader("sign");
        if (!verifySign(timestamp, sign, appSecret)) {
            System.err.println("[dingtalk] signature mismatch");
            context.response().setStatusCode(401).end();
            return;
        }

        JsonObject body;
        try {
            body = new JsonObject(context.body().asString());
        } catch (Exception e) {
            context.response().setStatusCode(400).end();
            return;
        }

        // 立即回 200
        context.response().putHeader("Content-Type", "application/json").end("{}");

        String sessionWebhook = body.getString("sessionWebhook");
        String text = body.getJsonObject("text", new JsonObject()).getString("content", "").strip();
        String userId = body.getString("senderStaffId", body.getString("senderId", "dingtalk"));

        if (sessionWebhook == null || text.isEmpty()) {
            return;
        }

        DingTalkClient client = new DingTalkClient(deps.vertx());
        deps.dispatcher()
                .dispatch(integration, userId, new JsonObject().put("content", text))
                .compose(reply -> client.sendMarkdown(sessionWebhook, "回复", (reply == null || reply.isBlank()) ? "(无回复)" : reply))
                .onFailure(e -> System.err.println("[dingtalk] dispatch/send failed: " + e.getMessage()));
    }

    /**
     * 验签: base64(HmacSHA256(timestamp + "\n" + appSecret, key=appSecret))
     */
    private static boolean verifySign(String timestamp, String sign, String appSecret) {
        if (timestamp == null || sign == null || appSecret.isEmpty()) {
            return false;
        }
        try {
            String stringToSign = timestamp + "\n" + appSecret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] data = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(data);
            return computed.equals(sign) || computed.equals(java.net.URLDecoder.decode(sign, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}
