package com.run.dagger.module;

import com.run.common.common_handler.LocaleHandler;
import com.run.common.common_handler.ResultHandler;
import com.run.common.failure_handler.RestFailureHandler;
import com.run.common.project.ProjectManage;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/13  23:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class RouteModule {

    @Singleton
    @Named("conversationAPIRoute")
    @Provides
    @Inject
    public Router conversationAPIRoute(Vertx vertx, @Named("mainRoute") Router router) {
        ProjectManage.setRouter(router);
        ProjectManage.setVertx(vertx);
        ProjectManage.setGetChildRouter(() -> Router.router(vertx));
        Router restAPI = Router.router(vertx);
        router.route("/conversation/api/*").subRouter(restAPI);
        restAPI.route()
                .handler(LocaleHandler::handle)
                .failureHandler(new RestFailureHandler())
                .handler(new ResultHandler());
        return restAPI;
    }

    @Singleton
    @Named("conversationUIRoute")
    @Provides
    @Inject
    public Router conversationUIRoute(Vertx vertx, @Named("mainRoute") Router router) {
        Router uiRoute = Router.router(vertx);
        router.route("/conversation/*").subRouter(uiRoute);
        return uiRoute;
    }

    @Singleton
    @Named("apiRoute")
    @Provides
    @Inject
    public Router apiRoute(Vertx vertx, @Named("mainRoute") Router router) {
        ProjectManage.setRouter(router);
        ProjectManage.setVertx(vertx);
        ProjectManage.setGetChildRouter(() -> Router.router(vertx));
        Router restAPI = Router.router(vertx);
        router.route("/admin/api/*").subRouter(restAPI);
        restAPI.route()
                .handler(LocaleHandler::handle)
                .failureHandler(new RestFailureHandler())
                .handler(new ResultHandler());
        return restAPI;
    }


    @Singleton
    @Named("uiRoute")
    @Provides
    @Inject
    public Router uiRoute(Vertx vertx, @Named("mainRoute") Router router) {
        Router uiRoute = Router.router(vertx);
        router.route("/admin/*").subRouter(uiRoute);
        return uiRoute;
    }


    @Singleton
    @Named("docRoute")
    @Provides
    @Inject
    public Router docRoute(Vertx vertx, @Named("mainRoute") Router router) {
        Router docRoute = Router.router(vertx);
        router.route("/doc/*").subRouter(docRoute);
        return docRoute;
    }


}
