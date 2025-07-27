package com.run.common.initialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.impl.StaticHandlerImpl;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/27  21:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class UIInitialization {
    private final Router router;

    @Inject
    public UIInitialization(@Named("uiRoute") Router uiRoute) {
        this.router = uiRoute;
    }

    public void init() {
        StaticHandlerImpl staticHandler = new StaticHandlerImpl(FileSystemAccess.RELATIVE, "ui/");
        router.get().handler(staticHandler);
    }
}
