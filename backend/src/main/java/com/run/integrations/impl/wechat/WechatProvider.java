package com.run.integrations.impl.wechat;

import com.run.dao.entity.Integration;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderInfo;
import com.run.integrations.wecom.WXBizMsgCrypt;
import com.run.integrations.wecom.WecomXml;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 微信公众号: GET 验证(signature+echostr) + POST 消息(明文/安全模式同腾讯 MsgCrypt)。
 * 回复用客服消息主动下发(需认证服务号)。凭证: appId/appSecret/token/aesKey(安全模式)。 }
 */
public class WechatProvider implements IIntegrationProvider {

    @Override
    public IntegrationProviderInfo info() {
        return new IntegrationProviderInfo("WECHAT", "微信公众号");
    }

    @Override
    public void verify(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        String token = config.getString("token", "");
        String signature = context.request().getParam("signature");
        String timestamp = context.request().getParam("timestamp");
        String nonce = context.request().getParam("nonce");
        String echostr = context.request().getParam("echostr");
        if (!sha1(token, timestamp, nonce).equals(signature)) {
            context.response().setStatusCode(401).end();
            return;
        }
        context.response().putHeader("Content-Type", "text/plain").end(echostr == null ? "" : echostr);
    }

    @Override
    public void handleMessage(RoutingContext context, Integration integration, IntegrationDeps deps) {
        JsonObject config = integration.decrypt();
        String token = config.getString("token", "");
        String aesKey = config.getString("aesKey", "");
        String appId = config.getString("appId", "");
        String appSecret = config.getString("appSecret", "");

        String body = context.body().asString();
        String message = body;
        String encrypt = WecomXml.extract(body, "Encrypt");
        if (encrypt != null && !aesKey.isEmpty()) {
            String msgSignature = context.request().getParam("msg_signature");
            String timestamp = context.request().getParam("timestamp");
            String nonce = context.request().getParam("nonce");
            WXBizMsgCrypt crypt = new WXBizMsgCrypt(token, aesKey, appId);
            if (!crypt.verifySignature(msgSignature, timestamp, nonce, encrypt)) {
                context.response().setStatusCode(401).end();
                return;
            }
            message = crypt.decrypt(encrypt);
        }

        String msgType = WecomXml.extract(message, "MsgType");
        String fromUser = WecomXml.extract(message, "FromUserName"); // openid
        String content = WecomXml.extract(message, "Content");

        // 5s 内回 success, 回复走客服消息主动下发
        context.response().putHeader("Content-Type", "text/plain").end("success");

        if (!"text".equals(msgType) || content == null) {
            return;
        }

        WechatClient client = new WechatClient(deps.vertx());
        deps.dispatcher()
                .dispatch(integration, fromUser, new JsonObject().put("content", content))
                .compose(reply -> client.sendCustomText(appId, appSecret, fromUser, (reply == null || reply.isBlank()) ? "(无回复)" : reply))
                .onFailure(e -> System.err.println("[wechat] dispatch/send failed: " + e.getMessage()));
    }

    /**
     * 公众号 GET 验证签名: SHA1(sort(token, timestamp, nonce))
     */
    private static String sha1(String token, String timestamp, String nonce) {
        try {
            String[] arr = {token, timestamp == null ? "" : timestamp, nonce == null ? "" : nonce};
            Arrays.sort(arr);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(String.join("", arr).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
