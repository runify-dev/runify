package com.run.route;

import com.run.common.route.IRoute;
import com.run.handler.integration.impl.IntegrationCallbackHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成入站回调路由(无 token, 平台签名校验)。
 * 统一 /integration/:integrationId/callback, GET=URL验证, POST=消息; 具体平台由 Integration.type 决定。 }
 */
public class IntegrationCallbackRoute implements IRoute {

    private final Router callbackRoute;
    private final IntegrationCallbackHandler callbackHandler;

    @Inject
    public IntegrationCallbackRoute(@Named("integrationCallbackRoute") Router callbackRoute,
                                    IntegrationCallbackHandler callbackHandler) {
        this.callbackRoute = callbackRoute;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void initRoute() {
        callbackRoute.get("/:integrationId/callback")
                .handler(callbackHandler::verify);
        callbackRoute.post("/:integrationId/callback")
                .handler(BodyHandler.create())
                .handler(callbackHandler::message);
    }

    @Override
    public void initOpenApi() {

    }
}
