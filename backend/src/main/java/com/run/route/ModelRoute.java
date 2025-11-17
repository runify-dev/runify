package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.model.IModelHandler;
import com.run.handler.model.impl.ModelFolderHandlerImpl;
import com.run.handler.model.impl.ModelHandlerImpl;
import com.run.handler.model.IModelFolderHandler;
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
    private final IModelFolderHandler modelFolderHandler;

    protected OpenAPI openAPI;

    @Inject
    public ModelRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                      TokenBasicAuthHandler tokenBasicAuthHandler,
                      ModelHandlerImpl modelHandler,
                      ModelFolderHandlerImpl modelFolderHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.modelHandler = modelHandler;
        this.modelFolderHandler = modelFolderHandler;
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

        apiRoute.get("/model/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::get);

        apiRoute.put("/model/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::edit);

        apiRoute.delete("/model/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::delete);

        apiRoute.post("/model/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::rename);

        apiRoute.post("/model/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelFolderHandler::rename);

        apiRoute.get("/model/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(modelFolderHandler::get);

        apiRoute.delete("/model/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(modelFolderHandler::delete);

        apiRoute.post("/model/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelFolderHandler::create);

        apiRoute.get("/model/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::tree);

        apiRoute.get("/model/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::list);

        apiRoute.post("/model/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::create);

        apiRoute.get("/model/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::listResourcePermission);

        apiRoute.put("/model/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(modelHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
