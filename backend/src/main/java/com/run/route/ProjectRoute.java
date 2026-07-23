package com.run.route;

import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.project.IProjectFolderHandler;
import com.run.handler.project.IProjectHandler;
import com.run.handler.project.impl.ProjectFolderHandlerImpl;
import com.run.handler.project.impl.ProjectHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  19:15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IProjectHandler iProjectHandler;

    private final IProjectFolderHandler iProjectFolderHandler;

    protected OpenAPI openAPI;

    @Inject
    public ProjectRoute(@Named("apiRoute") Router apiRoute, OpenAPI openAPI,
                        @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                        ProjectHandlerImpl iProjectHandler,
                        ProjectFolderHandlerImpl iProjectFolderHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.iProjectHandler = iProjectHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.iProjectFolderHandler = iProjectFolderHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/project/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_READ)
                                .addPermission(PermissionConstants.PROJECT_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::get);

//        apiRoute.put("/note/resources/:resourceId")
//                .handler(BodyHandler.create())
//                .handler(tokenBasicAuthHandler)
//                .handler(iProjectHandler::edit);

        apiRoute.get("/project/resources/:resourceId/error-response")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_READ)
                                .addPermission(PermissionConstants.PROJECT_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::getErrorResponse);

        apiRoute.put("/project/resources/:resourceId/error-response")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_EDIT)
                                .addPermission(PermissionConstants.PROJECT_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::editErrorResponse);

        apiRoute.delete("/project/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_DELETE)
                                .addPermission(PermissionConstants.PROJECT_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::delete);

        apiRoute.post("/project/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_EDIT)
                                .addPermission(PermissionConstants.PROJECT_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::rename);

        apiRoute.post("/project/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_EDIT)
                                .addPermission(PermissionConstants.PROJECT_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectFolderHandler::rename);

        apiRoute.get("/project/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_READ)
                                .addPermission(PermissionConstants.PROJECT_READ.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectFolderHandler::get);

        apiRoute.delete("/project/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_FOLDER_DELETE)
                                .addPermission(PermissionConstants.PROJECT_FOLDER_DELETE.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectFolderHandler::delete);

        apiRoute.post("/project/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_FOLDER_CREATE)
                                .addPermission(PermissionConstants.PROJECT_FOLDER_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectFolderHandler::create);

        apiRoute.get("/project/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::tree);

        apiRoute.get("/project/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_READ)
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .addRole(PermissionConstants.Role.USER)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::list);

        apiRoute.post("/project/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.PROJECT_CREATE)
                                .addPermission(PermissionConstants.PROJECT_CREATE.getFolderPermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(iProjectHandler::create);

        apiRoute.get("/project/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iProjectHandler::listResourcePermission);

        apiRoute.put("/project/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addRole(PermissionConstants.Role.ADMIN)
                        .build())
                .handler(iProjectHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
