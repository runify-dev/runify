package com.run.route;


import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.impl.ApplicationHandlerImpl;
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
        apiRoute.get("/application/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::get);

        apiRoute.delete("/application/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::delete);

        apiRoute.post("/application/folder/:folderId/resource/:resourceId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::rename);
        apiRoute.post("/application/folder/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::create);
        apiRoute.get("/application/folder/:folderId/tree")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::listTree);
        apiRoute.get("/application/folder/:folderId/resource")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::listResource);
        apiRoute.get("/application/folder/:folderId/star")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::listStar);
        apiRoute.get("/application/folder/:folderId/shared")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::listShared);
    }

    @Override
    public void initOpenApi() {

    }
}
