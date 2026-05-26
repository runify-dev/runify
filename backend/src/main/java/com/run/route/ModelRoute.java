package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
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
                      @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
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
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_READ)
                                .addPermission(PermissionConstants.MODEL_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::get);

        apiRoute.put("/model/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_EDIT)
                                .addPermission(PermissionConstants.MODEL_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::edit);

        apiRoute.delete("/model/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_DELETE)
                                .addPermission(PermissionConstants.MODEL_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::delete);

        apiRoute.post("/model/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_EDIT)
                                .addPermission(PermissionConstants.MODEL_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::rename);

        apiRoute.post("/model/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_EDIT)
                                .addPermission(PermissionConstants.MODEL_EDIT.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelFolderHandler::rename);

        apiRoute.get("/model/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_READ)
                                .addPermission(PermissionConstants.MODEL_READ.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelFolderHandler::get);

        apiRoute.delete("/model/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_DELETE)
                                .addPermission(PermissionConstants.MODEL_DELETE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelFolderHandler::delete);

        apiRoute.post("/model/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_FOLDER_CREATE)
                                .addPermission(PermissionConstants.MODEL_FOLDER_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelFolderHandler::create);

        apiRoute.get("/model/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::tree);

        apiRoute.get("/model/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::list);

        apiRoute.post("/model/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_CREATE)
                                .addPermission(PermissionConstants.MODEL_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::create);

        apiRoute.post("/model/folders/:folderId/resources/validate")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.MODEL_CREATE)
                                .addPermission(PermissionConstants.MODEL_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(modelHandler::validate);

        apiRoute.get("/model/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(modelHandler::listResourcePermission);

        apiRoute.put("/model/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(modelHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
