package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.tree.INodeHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:45}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeRoute implements IRoute {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    @Inject
    protected INodeHandler iNodeHandler;


    @Override
    public void initRoute() {
        apiRoute.get("/:resource/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::get);

        apiRoute.delete("/:resource/folder/:folderId/resource/:resourceId")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::delete);

        apiRoute.post("/:resource/folder/:folderId/resource/:resourceId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::rename);

        apiRoute.post("/:resource/folder/:folderId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::create);

        apiRoute.get("/:resource/folder/:folderId/tree")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::listTree);
        apiRoute.get("/:resource/folder/:folderId/resource")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::listResource);
        apiRoute.get("/:resource/folder/:folderId/star")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::listStar);
        apiRoute.get("/:resource/folder/:folderId/shared")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::listShared);

    }

    @Override
    public void initOpenApi() {

    }


}
