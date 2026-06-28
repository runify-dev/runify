package com.run.integrations.impl.wecomstream;

import com.run.dao.entity.Integration;
import com.run.dao.mapper.IntegrationMapper;
import com.run.handler.integration.impl.ChatIntegrationMessageDispatcher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/28  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 企业微信智能机器人长连接(WECOM_STREAM)管理: 启动时拉起所有启用的集成 WebSocket 连接;
 * 登录/启停时增量管理。结构对齐 WeixinPollerManager(同为自驱动常驻连接, 不走 IIntegrationProvider 回调接口)。 }
 */
@Singleton
public class WecomStreamManager {

    private final IntegrationMapper integrationMapper;
    private final ChatIntegrationMessageDispatcher dispatcher;
    private final Vertx vertx;
    private final Map<String, WecomStreamConnection> connections = new ConcurrentHashMap<>();

    @Inject
    public WecomStreamManager(IntegrationMapper integrationMapper, ChatIntegrationMessageDispatcher dispatcher, Vertx vertx) {
        this.integrationMapper = integrationMapper;
        this.dispatcher = dispatcher;
        this.vertx = vertx;
    }

    /**
     * 启动时拉起所有 WECOM_STREAM 集成
     */
    public void startAll() {
        integrationMapper.list(field(Integration::getType).eq("WECOM_STREAM"))
                .onSuccess(list -> {
                    for (Integration integration : list) {
                        try {
                            start(integration);
                        } catch (Exception e) {
                            System.err.println("[wecom-stream] start failed: " + e.getMessage());
                        }
                    }
                })
                .onFailure(e -> System.err.println("[wecom-stream] load integrations failed: " + e.getMessage()));
    }

    public void start(Integration integration) {
        JsonObject config = integration.decrypt();
        String botId = config.getString("botId", "");
        String secret = config.getString("secret", "");
        if (botId.isEmpty() || secret.isEmpty()) {
            return;
        }
        if (integration.getEnabled() != null && !integration.getEnabled()) {
            return;
        }
        String id = integration.getId().toString();
        stop(id);
        WecomStreamConnection conn = new WecomStreamConnection(integration, botId, secret, dispatcher, vertx);
        connections.put(id, conn);
        conn.start();
    }

    public void stop(String integrationId) {
        WecomStreamConnection conn = connections.remove(integrationId);
        if (conn != null) {
            conn.stop();
        }
    }
}
