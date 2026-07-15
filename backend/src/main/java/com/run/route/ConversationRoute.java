package com.run.route;

import com.run.auth.ConversationAuthenticator;
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
    private final ConversationAuthenticator conversationAuthenticator = new ConversationAuthenticator();

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
        apiRoute.get("/application")
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::query);

        apiRoute.get("/application/:applicationId")
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::application);

        apiRoute.get("/application/:applicationId/auth-profile")
                .handler(conversationHandler::authProfile);

        apiRoute.get("/userProfile")
                .handler(tokenBasicAuthHandler)
                .handler(conversationHandler::userProfile);

        apiRoute.get("/application/:applicationId/section-fact")
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::mySections);

        apiRoute.post("/anonymousLogin")
                .handler(BodyHandler.create())
                .handler(conversationHandler::anonymousLogin);

        apiRoute.post("/login")
                .handler(BodyHandler.create())
                .handler(conversationHandler::login);

        apiRoute.post("/application/:applicationId/conversation")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::createConversation);

        apiRoute.get("/application/:applicationId/conversation")
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::pageConversation);

        apiRoute.get("/application/:applicationId/conversation/:conversationId/message")
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::pageMessage);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/chat")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::conversation);

        apiRoute.get("/application/:applicationId/conversation/:conversationId/status")
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::statusStream);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/resume-stream")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::resumeStream);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/cancel")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::cancel);


        apiRoute.delete("/application/:applicationId/conversation/:conversationId")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::delConversation);

        apiRoute.post("/application/:applicationId/conversation/:conversationId/modify-name")
                .handler(BodyHandler.create())
                .handler(tokenBasicAuthHandler)
                .handler(conversationAuthenticator)
                .handler(conversationHandler::modifyName);

        apiRoute.get("/application/:applicationId/embed")
                .handler(conversationHandler::embed);
    }

    @Override
    public void initOpenApi() {

    }
}
