package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;
import com.run.common.common_handler.ResultHandler;
import com.run.common.failure_handler.RestFailureHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  23:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class RouteModule extends AbstractModule {
    @Inject
    private Vertx vertx;

    @Override
    protected void configure() {
        Router mainRoute = Router.router(vertx);
        Router uiRoute = Router.router(vertx);
        Router restAPI = Router.router(vertx);
        Router docRoute = Router.router(vertx);
        restAPI.route()
                .failureHandler(new RestFailureHandler())
                .handler(new ResultHandler());
        mainRoute.route("/api/*").subRouter(restAPI);
        mainRoute.route("/ui/*").subRouter(uiRoute);
        mainRoute.route("/doc/*").subRouter(docRoute);
        bind(Router.class).annotatedWith(Names.named("mainRoute")).toInstance(mainRoute);
        bind(Router.class).annotatedWith(Names.named("apiRoute")).toInstance(restAPI);
        bind(Router.class).annotatedWith(Names.named("uiRoute")).toInstance(uiRoute);
        bind(Router.class).annotatedWith(Names.named("docRoute")).toInstance(docRoute);
    }
}
