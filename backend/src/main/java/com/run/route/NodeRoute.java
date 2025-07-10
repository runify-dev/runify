package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.node.INodeHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:45}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeRoute implements IRoute  {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    @Inject
    protected INodeHandler iNodeHandler;


    @Override
    public void initRoute() {
        apiRoute.get("/node")
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::list);
        apiRoute.post("/node")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::create);
        apiRoute.put("/node/:node_id")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::edit);
        apiRoute.delete("/node/:node_id")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(iNodeHandler::delete);
    }

    @Override
    public void initOpenApi() {

    }


}
