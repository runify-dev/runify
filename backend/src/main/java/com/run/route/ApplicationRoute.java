package com.run.route;


import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.impl.ApplicationHandlerImpl;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.impl.KnowledgeHandlerImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationRoute implements IRoute {

    private final Router apiRoute;

    private final Pool pool;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private IApplicationHandler applicationHandler;

    @Inject
    public ApplicationRoute(@Named("apiRoute") Router apiRoute, Pool pool, TokenBasicAuthHandler tokenBasicAuthHandler, ApplicationHandlerImpl applicationHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.applicationHandler = applicationHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.put("/application/folder/:folderId/resource/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::edit);
    }

    @Override
    public void initOpenApi() {

    }
}
