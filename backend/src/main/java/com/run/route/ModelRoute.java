package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.impl.KnowledgeHandlerImpl;
import com.run.handler.model.IModelHandler;
import com.run.handler.model.impl.ModelHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

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
        apiRoute.put("/model/folder/:folderId/resource/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::edit);
        apiRoute.get("/model/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::get);
        apiRoute.delete("/model/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::delete);
        apiRoute.post("/model/folder/:folderId/resource/:resourceId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::rename);
        apiRoute.post("/model/folder/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::create);
        apiRoute.get("/model/folder/:folderId/tree")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listTree);
        apiRoute.get("/model/folder/:folderId/resource")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listResource);
        apiRoute.get("/model/folder/:folderId/star")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listStar);
        apiRoute.get("/model/folder/:folderId/shared")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listShared);
    }

    @Override
    public void initOpenApi() {

    }
}
