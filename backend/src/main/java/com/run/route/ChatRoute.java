package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.application.impl.ApplicationHandlerImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Pool;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Named;

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

    private TokenBasicAuthHandler tokenBasicAuthHandler;

    @Inject
    public ChatRoute(@Named("apiRoute") Router apiRoute, Pool pool, ApplicationHandlerImpl applicationHandler,
                     @Named("tokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.applicationHandler = applicationHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @SneakyThrows
    private void chat() {
        apiRoute.post("/application/:applicationId/conversation")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::createConversation);

        apiRoute.post("/application/:applicationId/conversation/:conversationId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::chat);

        apiRoute.delete("/application/:applicationId/conversation/:conversationId")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::deleteConversation);

        apiRoute.get("/application/:applicationId/conversation/:conversationId/status")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::statusStream);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/resume-stream")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::resumeStream);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/cancel")
                .handler(applicationHandler::cancel);

        apiRoute.put("/application/:applicationId/conversation/:conversationId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::modifyConversationName);

        apiRoute.get("/application/:applicationId/conversation")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::pageConversation);

        apiRoute.get("/application/:applicationId/conversation/mine")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::mineConversation);

        apiRoute.get("/application/:applicationId/conversation/:conversationId/message")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::pageConversationMessage);

        apiRoute.get("/application/:applicationId/overview")
                .handler(tokenBasicAuthHandler)
                .handler(applicationHandler::overview);
    }


    @Override
    public void initRoute() {
        chat();
    }

    @Override
    public void initOpenApi() {

    }
}
