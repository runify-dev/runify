package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.impl.KnowledgeHandlerImpl;
import com.run.handler.model.IModelHandler;
import com.run.handler.model.impl.ModelHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/5  22:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IModelHandler modelHandler;

    protected OpenAPI openAPI;

    @Inject
    public ModelRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                      TokenBasicAuthHandler tokenBasicAuthHandler,
                      ModelHandlerImpl modelHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.modelHandler = modelHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/model/provider")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::getProvider);
        apiRoute.get("/model/:provider/template")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::getModelList);
        apiRoute.get("/model/:provider/type")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listModelType);
    }

    @Override
    public void initOpenApi() {

    }
}
