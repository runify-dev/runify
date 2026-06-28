package com.run.integrations.impl.weixin;

import com.run.dao.entity.Integration;
import com.run.dao.mapper.FileMapper;
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
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 微信(个人号/iLink)长轮询管理: 启动时拉起所有启用的 WEIXIN 集成 poller; 登录成功/启停时增量管理 }
 */
@Singleton
public class WeixinPollerManager {

    private final IntegrationMapper integrationMapper;
    private final ChatIntegrationMessageDispatcher dispatcher;
    private final FileMapper fileMapper;
    private final Vertx vertx;
    private final Map<String, WeixinPoller> pollers = new ConcurrentHashMap<>();

    @Inject
    public WeixinPollerManager(IntegrationMapper integrationMapper, ChatIntegrationMessageDispatcher dispatcher,
                               FileMapper fileMapper, Vertx vertx) {
        this.integrationMapper = integrationMapper;
        this.dispatcher = dispatcher;
        this.fileMapper = fileMapper;
        this.vertx = vertx;
    }

    /**
     * 启动时拉起所有 WEIXIN 集成
     */
    public void startAll() {
        integrationMapper.list(field(Integration::getType).eq("WEIXIN"))
                .onSuccess(list -> {
                    for (Integration integration : list) {
                        try {
                            start(integration);
                        } catch (Exception e) {
                            System.err.println("[weixin] start poller failed: " + e.getMessage());
                        }
                    }
                })
                .onFailure(e -> System.err.println("[weixin] load integrations failed: " + e.getMessage()));
    }

    public void start(Integration integration) {
        JsonObject config = integration.decrypt();
        String token = config.getString("token", "");
        if (token.isEmpty()) {
            return;
        }
        if (integration.getEnabled() != null && !integration.getEnabled()) {
            return;
        }
        String id = integration.getId().toString();
        stop(id);
        WeixinPoller poller = new WeixinPoller(integration,
                config.getString("baseUrl", IlinkClient.ILINK_BASE_URL),
                token,
                config.getString("accountId", ""),
                dispatcher,
                fileMapper,
                vertx);
        pollers.put(id, poller);
        poller.start();
    }

    public void stop(String integrationId) {
        WeixinPoller poller = pollers.remove(integrationId);
        if (poller != null) {
            poller.stop();
        }
    }
}
