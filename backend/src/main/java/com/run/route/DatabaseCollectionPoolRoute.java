package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.project.IDatabaseCollectionPoolHandler;
import com.run.handler.project.IProcessorHandler;
import com.run.handler.project.impl.DatabaseCollectionPoolHandlerImpl;
import com.run.handler.project.impl.ProcessorHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DatabaseCollectionPoolRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IDatabaseCollectionPoolHandler databaseCollectionPoolHandler;

    protected OpenAPI openAPI;

    @Inject
    public DatabaseCollectionPoolRoute(@Named("apiRoute") Router apiRoute,
                                       OpenAPI openAPI,
                                       TokenBasicAuthHandler tokenBasicAuthHandler,
                                       DatabaseCollectionPoolHandlerImpl databaseCollectionPoolHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.databaseCollectionPoolHandler = databaseCollectionPoolHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.post("/project/:projectId/database-collection-pool")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(databaseCollectionPoolHandler::create);

        apiRoute.get("/project/:projectId/database-collection-pool")
                .handler(tokenBasicAuthHandler)
                .handler(databaseCollectionPoolHandler::page);

    }

    @Override
    public void initOpenApi() {

    }
}