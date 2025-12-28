package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
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
                        TokenBasicAuthHandler tokenBasicAuthHandler,
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
                .handler(iProjectHandler::get);

//        apiRoute.put("/note/resources/:resourceId")
//                .handler(BodyHandler.create())
//                .handler(tokenBasicAuthHandler)
//                .handler(iProjectHandler::edit);

        apiRoute.delete("/project/resources/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::delete);

        apiRoute.post("/project/resources/:resourceId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::rename);

        apiRoute.post("/project/folders/:folderId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iProjectFolderHandler::rename);

        apiRoute.get("/project/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectFolderHandler::get);

        apiRoute.delete("/project/folders/:folderId")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectFolderHandler::delete);

        apiRoute.post("/project/folders/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iProjectFolderHandler::create);

        apiRoute.get("/project/folders/:folderId/subtree")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::tree);

        apiRoute.get("/project/folders/:folderId/resources")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::list);

        apiRoute.post("/project/folders/:folderId/resources")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::create);

        apiRoute.get("/project/permissions/:userId")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::listResourcePermission);

        apiRoute.put("/project/permissions/:userId/authorization/:resourceId/:permission")
                .handler(tokenBasicAuthHandler)
                .handler(iProjectHandler::authResourcePermission);
    }

    @Override
    public void initOpenApi() {

    }
}
