package com.run.common.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.run.common.route.IRoute;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.impl.StaticHandlerImpl;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/27  22:23}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DocRoute implements IRoute {

    protected Router docRoute;

    protected OpenAPI openAPI;

    @Inject
    public DocRoute(@Named("docRoute") Router docRoute, OpenAPI openAPI) {
        this.docRoute = docRoute;
        this.openAPI = openAPI;
    }

    @Override
    public void initRoute() {
        StaticHandlerImpl staticHandler = new StaticHandlerImpl(FileSystemAccess.RELATIVE, "doc/");
        docRoute.get().handler(staticHandler);
        docRoute.get("/swagger.json").handler(context -> {
            context.response().putHeader("content-type", "application/json;charset=utf-8");
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String json = objectMapper.writeValueAsString(openAPI);
                context.end(json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void initOpenApi() {

    }


}
