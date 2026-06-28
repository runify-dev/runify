package com.run.handler.integration.impl;

import com.run.dao.mapper.FileMapper;
import com.run.dao.mapper.IntegrationMapper;
import com.run.integrations.IIntegrationProvider;
import com.run.integrations.IntegrationDeps;
import com.run.integrations.IntegrationProviderConstants;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成回调入口(薄): 按 Integration.type 解析供应商并委派, 各平台逻辑在 com.run.integrations 各自实现 }
 */
public class IntegrationCallbackHandler {

    private final IntegrationMapper integrationMapper;
    private final ChatIntegrationMessageDispatcher dispatcher;
    private final FileMapper fileMapper;
    private final Vertx vertx;

    @Inject
    public IntegrationCallbackHandler(IntegrationMapper integrationMapper,
                                      ChatIntegrationMessageDispatcher dispatcher,
                                      FileMapper fileMapper,
                                      Vertx vertx) {
        this.integrationMapper = integrationMapper;
        this.dispatcher = dispatcher;
        this.fileMapper = fileMapper;
        this.vertx = vertx;
    }

    public void verify(RoutingContext context) {
        dispatch(context, IIntegrationProvider::verify);
    }

    public void message(RoutingContext context) {
        dispatch(context, IIntegrationProvider::handleMessage);
    }

    private void dispatch(RoutingContext context, ProviderCall call) {
        String integrationId = context.pathParam("integrationId");
        integrationMapper.getById(integrationId).onSuccess(integration -> {
            if (integration == null) {
                context.response().setStatusCode(404).end();
                return;
            }
            IIntegrationProvider provider = IntegrationProviderConstants.of(integration.getType());
            if (provider == null) {
                context.response().setStatusCode(400).end("unknown integration type: " + integration.getType());
                return;
            }
            try {
                call.accept(provider, context, integration, new IntegrationDeps(vertx, dispatcher, fileMapper));
            } catch (Exception e) {
                System.err.println("[integration] callback error: " + e.getMessage());
                if (!context.response().ended()) {
                    context.response().setStatusCode(500).end();
                }
            }
        }).onFailure(e -> context.response().setStatusCode(500).end());
    }

    @FunctionalInterface
    private interface ProviderCall {
        void accept(IIntegrationProvider provider, RoutingContext context, com.run.dao.entity.Integration integration, IntegrationDeps deps);
    }
}
