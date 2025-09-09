package com.run.route;


import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.tree.INodeHandler;
import com.run.handler.tree.impl.NodeHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:45}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    protected INodeHandler iNodeHandler;

    @Inject
    public NodeRoute(@Named("apiRoute") Router apiRoute,
                     OpenAPI openAPI,
                     TokenBasicAuthHandler tokenBasicAuthHandler,
                     NodeHandlerImpl nodeHandler) {
        this.apiRoute = apiRoute;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.iNodeHandler = nodeHandler;
    }

    @Override
    public void initRoute() {


    }

    @Override
    public void initOpenApi() {

    }


}
