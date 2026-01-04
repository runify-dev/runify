package com.run.common.initialization;

import com.run.common.util.ResourceLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.impl.StaticHandlerImpl;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  21:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class UIInitialization {
    private final Router router;
    private final Vertx vertx;
    private final Router mainRouter;
    List<String> adminResources = ResourceLoader.getAdminResources();

    @Inject
    public UIInitialization(@Named("mainRoute") Router mainRouter,
                            @Named("uiRoute") Router uiRoute,
                            Vertx vertx) {
        this.router = uiRoute;
        this.vertx = vertx;
        this.mainRouter = mainRouter;
    }

    public void init() {
        StaticHandlerImpl staticHandler = new StaticHandlerImpl(FileSystemAccess.RELATIVE, "admin/");
        router.get().handler(staticHandler);
        router.route().last().handler(context -> {
            String path = context.request().path();
            Optional<String> first = adminResources.stream().filter(path::endsWith).findFirst();
            if (first.isPresent()) {
                context.redirect("/admin/" + first.get());
            } else {
                vertx.fileSystem().readFile("admin/index.html")
                        .onSuccess(result -> {
                            context.response()
                                    .putHeader("Content-Type", "text/html")
                                    .end(result);
                        }).onFailure(context::fail);
            }

        });

    }
}
