package com.run.auth;

import com.run.common.exception.ApiException;
import com.run.common.exception.ForbiddenException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;

@Getter
public class ConversationAuthenticator implements Handler<RoutingContext> {


    @Override
    public void handle(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        JsonArray applicationIds = context.user().get("applicationIds");
        if (applicationIds.contains(applicationId)) {
            context.next();
        } else {
            context.fail(new ForbiddenException("权限不足"));
        }
    }
}
