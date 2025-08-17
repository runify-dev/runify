package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.application.impl.ApplicationHandlerImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/30  18:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ChatRoute implements IRoute {

    protected Router apiRoute;

    protected Pool pool;

    private ApplicationHandlerImpl applicationHandler;

    private TokenBasicAuthHandler tokenBasicAuthHandler;

    @Inject
    public ChatRoute(@Named("apiRoute") Router apiRoute, Pool pool, ApplicationHandlerImpl applicationHandler,
                     TokenBasicAuthHandler tokenBasicAuthHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.applicationHandler = applicationHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @SneakyThrows
    private void chat() {
        apiRoute.post("/application/folder/:folderId/resource/:applicationId/conversation")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::chat);
    }


    @Override
    public void initRoute() {
        chat();
    }

    @Override
    public void initOpenApi() {

    }
}
