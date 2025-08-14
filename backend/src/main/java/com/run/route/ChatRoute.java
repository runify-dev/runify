package com.run.route;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.run.common.openai.request.message.UserMessage;
import com.run.common.route.IRoute;
import com.run.dao.mapper.ApplicationMapper;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.impl.ApplicationHandlerImpl;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/30  18:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ChatRoute implements IRoute {

    protected Router apiRoute;

    protected Pool pool;

    private ApplicationHandlerImpl applicationHandler;

    @Inject
    public ChatRoute(@Named("apiRoute") Router apiRoute, Pool pool, ApplicationHandlerImpl applicationHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.applicationHandler = applicationHandler;
    }

    @SneakyThrows
    private void chat() {
        apiRoute.post("/application/folder/:folderId/resource/:resourceId/chat")
                .handler(BodyHandler.create())
                .handler(applicationHandler::chat);
    }


    @Override
    public void initRoute() {
        chat();
    }

    @Override
    public void initOpenApi() {

    }
}
