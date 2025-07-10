package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.run.auth.TokenBasicAuthHandler;
import io.vertx.sqlclient.Pool;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TokenAuthHandlerModule  extends AbstractModule {
    @Inject
    private Pool pool;
    @Override
    protected void configure() {
        TokenBasicAuthHandler tokenBasicAuthHandler = new TokenBasicAuthHandler(pool);
        bind(Key.get(TokenBasicAuthHandler.class)).toInstance(tokenBasicAuthHandler);
    }
}
