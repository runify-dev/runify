package com.run.auth;

import com.run.auth.provider.TokenProvider;

import com.run.common.result.Result;
import com.run.sql.dialect.SQLDialect;
import io.vertx.core.Future;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler;
import io.vertx.sqlclient.Pool;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/4  23:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenBasicAuthHandler extends HTTPAuthorizationHandler<AuthenticationProvider> {


    public TokenBasicAuthHandler(AuthenticationProvider authProvider) {
        super(authProvider, Type.BEARER, null);
    }


    @Override
    public Future<User> authenticate(RoutingContext context) {
        return parseAuthorization(context)
                .compose(token -> this.authProvider.authenticate(new TokenCredentials(token)))
                .onFailure(e -> {
                    context.response().setStatusCode(401);
                    context.end(Result.error("不合法的token").toBuffer());
                });
    }
}
