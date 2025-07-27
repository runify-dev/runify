package com.run.route;


import com.run.common.route.IRoute;
import io.vertx.ext.web.Router;
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

    @Inject
    public ApplicationRoute(@Named("apiRoute") Router apiRoute, Pool pool) {
        this.apiRoute = apiRoute;
        this.pool = pool;
    }

    @Override
    public void initRoute() {

    }

    @Override
    public void initOpenApi() {

    }
}
