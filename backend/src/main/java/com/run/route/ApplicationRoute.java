package com.run.route;


import com.run.auth.AggregatePermission;
import com.run.auth.Authenticator;
import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.constants.PermissionConstants;
import com.run.common.route.IRoute;
import com.run.handler.application.IApplicationFolderHandler;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.impl.ApplicationFolderHandlerImpl;
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

    protected final TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IApplicationHandler applicationHandler;
    private final IApplicationFolderHandler applicationFolderHandler;

    @Inject
    public ApplicationRoute(@Named("apiRoute") Router apiRoute, Pool pool,
                            @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                            ApplicationHandlerImpl applicationHandler,
                            ApplicationFolderHandlerImpl applicationFolderHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.applicationHandler = applicationHandler;
        this.applicationFolderHandler = applicationFolderHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/application/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::get);

        apiRoute.put("/application/resources/:resourceId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(Authenticator.builder()
                        .addPermission(AggregatePermission.builder()
                                .addPermission(PermissionConstants.APPLICATION_EDIT)
                                .addPermission(PermissionConstants.APPLICATION_EDIT.getResourcePermission())
                                .compare(PermissionConstants.Compare.AND).build())
                        .addRole(PermissionConstants.Role.ADMIN)
                        .compare(PermissionConstants.Compare.OR)
                        .build())
                .handler(applicationHandler::edit);

        apiRoute.delete("/application/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::delete);

        apiRoute.post("/application/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::rename);

        apiRoute.post("/application/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationFolderHandler::rename);

        apiRoute.get("/application/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationFolderHandler::get);

        apiRoute.delete("/application/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationFolderHandler::delete);

        apiRoute.post("/application/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationFolderHandler::create);

        apiRoute.get("/application/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::tree);

        apiRoute.get("/application/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::list);

        apiRoute.post("/application/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::create);

        apiRoute.get("/application/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::listResourcePermission);
        apiRoute.put("/application/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
