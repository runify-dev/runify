package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.project.IProcessorHandler;
import com.run.handler.project.impl.ProcessorHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  20:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProcessorRoute implements IRoute {
    protected Router apiRoute;

    protected TokenBasicAuthHandler tokenBasicAuthHandler;

    private final IProcessorHandler processorHandler;

    protected OpenAPI openAPI;

    @Inject
    public ProcessorRoute(@Named("apiRoute") Router apiRoute,
                          OpenAPI openAPI,
                          @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                          ProcessorHandlerImpl processorHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.processorHandler = processorHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {

        apiRoute.post("/project/:projectId/processor")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::create);

        apiRoute.get("/project/:projectId/processor")
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::page);

        apiRoute.get("/project/:projectId/processor/:processorId")
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::get);

        apiRoute.put("/project/:projectId/processor/:processorId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::edit);

        apiRoute.post("/project/:projectId/processor/:processorId/deploy")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::deploy);

        apiRoute.post("/project/:projectId/processor/:processorId/undeploy")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(processorHandler::undeploy);

    }

    @Override
    public void initOpenApi() {

    }
}
