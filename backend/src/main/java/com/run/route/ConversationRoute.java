package com.run.route;

import com.run.auth.TokenBasicAuthHandler;
import com.run.common.route.IRoute;
import com.run.handler.conversation.IConversationHandler;
import com.run.handler.conversation.impl.ConversationHandlerImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/9  21:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConversationRoute implements IRoute {
    protected Router apiRoute;

    protected Pool pool;

    private final IConversationHandler conversationHandler;

    private final TokenBasicAuthHandler tokenBasicAuthHandler;


    @Inject
    public ConversationRoute(@Named("conversationAPIRoute") Router apiRoute,
                             Pool pool,
                             ConversationHandlerImpl conversationHandler,
                             @Named("conversationTokenBasicAuthHandler") TokenBasicAuthHandler tokenBasicAuthHandler) {
        this.apiRoute = apiRoute;
        this.pool = pool;
        this.conversationHandler = conversationHandler;
        this.tokenBasicAuthHandler = tokenBasicAuthHandler;
    }

    @Override
    public void initRoute() {
        apiRoute.get("/config")
                .handler(conversationHandler::config);

        apiRoute.post("/anonymousLogin")
                .handler(BodyHandler.create())
                .handler(conversationHandler::anonymousLogin);

        apiRoute.post("/conversation")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::createConversation);

        apiRoute.post("/conversation/:conversationId/chat")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::conversation);

        apiRoute.delete("/conversation/:conversationId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::delConversation);

        apiRoute.post("/conversation/:conversationId/rename")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::rename);
    }

    @Override
    public void initOpenApi() {

    }
}
