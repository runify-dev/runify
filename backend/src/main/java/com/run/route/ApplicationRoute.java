package com.run.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.common.route.IRoute;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationRoute implements IRoute {
    @Inject
    @Named("apiRoute")
    protected Router apiRoute;
    @Inject
    protected Pool pool;


    @Override
    public void initRoute() {

    }

    @Override
    public void initOpenApi() {

    }
}
