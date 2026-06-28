package com.run.handler.integration.impl;

import com.run.common.result.Result;
import com.run.dao.entity.Integration;
import com.run.dao.mapper.IntegrationMapper;
import com.run.integrations.impl.weixin.IlinkClient;
import com.run.integrations.impl.weixin.WeixinPollerManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.LocalDateTime;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 微信(个人号/iLink)扫码登录: 取二维码 + 轮询状态; confirmed 时把凭证存入集成并拉起 poller }
 */
public class WeixinAuthHandler {

    private final IntegrationMapper integrationMapper;
    private final WeixinPollerManager pollerManager;
    private final Vertx vertx;

    @Inject
    public WeixinAuthHandler(IntegrationMapper integrationMapper, WeixinPollerManager pollerManager, Vertx vertx) {
        this.integrationMapper = integrationMapper;
        this.pollerManager = pollerManager;
        this.vertx = vertx;
    }

    /**
     * 获取登录二维码 -> {qrcode, qrcode_img_content}
     */
    public void qrcode(RoutingContext context) {
        vertx.executeBlocking(IlinkClient::getBotQrcode, false)
                .onSuccess(qr -> context.end(Result.success(qr).toBuffer()))
                .onFailure(context::fail);
    }

    /**
     * 轮询扫码状态; confirmed 时保存凭证并启动 poller
     */
    public void qrcodeStatus(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        String qrcode = context.request().getParam("qrcode");
        String baseUrl = context.request().getParam("baseUrl");
        String effectiveBase = (baseUrl == null || baseUrl.isEmpty()) ? IlinkClient.ILINK_BASE_URL : baseUrl;

        vertx.executeBlocking(() -> IlinkClient.getQrcodeStatus(effectiveBase, qrcode), false)
                .compose(status -> {
                    if (!"confirmed".equals(status.getString("status"))) {
                        return io.vertx.core.Future.succeededFuture(status);
                    }
                    return integrationMapper.getById(resourceId).compose(integration -> {
                        if (integration == null) {
                            System.err.println("[weixin] qrstatus confirmed but integration not found id=" + resourceId);
                            return io.vertx.core.Future.succeededFuture(status);
                        }
                        JsonObject config = integration.decrypt();
                        config.put("accountId", status.getString("ilink_bot_id", ""));
                        config.put("token", status.getString("bot_token", ""));
                        config.put("baseUrl", status.getString("baseurl", IlinkClient.ILINK_BASE_URL));
                        config.put("userId", status.getString("ilink_user_id", ""));
                        integration.setConfig(integration.encrypt(config));
                        integration.setUpdateTime(LocalDateTime.now());
                        return integrationMapper.update(integration).map(ok -> {
                            pollerManager.start(integration);
                            return status;
                        });
                    });
                })
                .onSuccess(status -> context.end(Result.success(status).toBuffer()))
                .onFailure(e -> {
                    System.err.println("[weixin] qrstatus error: " + e.getMessage());
                    context.fail(e);
                });
    }
}
