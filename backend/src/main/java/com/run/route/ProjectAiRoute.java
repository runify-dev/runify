package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.project.impl.ProjectAiHandlerImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * 项目级 AI 生成持久化路由：蓝图 / 会话 / 任务台账 / 消息流。
 * 挂在 admin/api 下，统一 token 鉴权。
 */
public class ProjectAiRoute implements IRoute {
    protected Router apiRoute;
    protected TokenBasicAuthHandler tokenBasicAuthHandler;
    protected OpenAPI openAPI;
    private final ProjectAiHandlerImpl projectAiHandler;

    @Inject
    public ProjectAiRoute(@Named("apiRoute") Router apiRoute,
                          OpenAPI openAPI,
                          @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler,
                          ProjectAiHandlerImpl projectAiHandler) {
        this.apiRoute = apiRoute;
        this.openAPI = openAPI;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
        this.projectAiHandler = projectAiHandler;
    }

    @Override
    public void initRoute() {
        // L1 蓝图（1:1 project）
        apiRoute.get("/project/:projectId/ai/blueprint")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::getBlueprint);
        apiRoute.put("/project/:projectId/ai/blueprint")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::upsertBlueprint);

        // L2 会话（1:N project）
        apiRoute.post("/project/:projectId/ai/session")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::createSession);
        apiRoute.get("/project/:projectId/ai/session")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::listSessions);
        apiRoute.get("/project/:projectId/ai/session/:sessionId")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::getSession);
        apiRoute.put("/project/:projectId/ai/session/:sessionId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::updateSession);
        apiRoute.delete("/project/:projectId/ai/session/:sessionId")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::deleteSession);

        // L3 任务台账（1:N session）
        apiRoute.post("/project/:projectId/ai/session/:sessionId/task")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::createTask);
        apiRoute.get("/project/:projectId/ai/session/:sessionId/task")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::listTasks);
        apiRoute.get("/project/:projectId/ai/task/:taskId")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::getTask);
        apiRoute.put("/project/:projectId/ai/task/:taskId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::updateTask);

        // 统一消息流（append-only）
        apiRoute.post("/project/:projectId/ai/message")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::appendMessage);
        apiRoute.get("/project/:projectId/ai/message")
                .handler(tokenBasicAuthHandler)
                .handler(projectAiHandler::listMessages);
    }

    @Override
    public void initOpenApi() {

    }
}
