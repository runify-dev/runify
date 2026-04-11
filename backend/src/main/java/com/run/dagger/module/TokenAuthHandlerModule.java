package com.run.dagger.module;


import com.run.auth.TokenBasicAuthHandler;
import com.run.auth.provider.ConversationTokenProvider;
import dagger.Module;
import dagger.Provides;
import io.vertx.sqlclient.Pool;
import org.jooq.SQLDialect;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class TokenAuthHandlerModule {
    @Inject
    @Singleton
    @Provides
    @Named("tokenBasicAuthHandler")
    public TokenBasicAuthHandler getTokenBasicAuthHandler(Pool pool, SQLDialect dbType) {
        return new TokenBasicAuthHandler(pool, dbType);
    }

    @Inject
    @Singleton
    @Provides
    @Named("conversationTokenBasicAuthHandler")
    public TokenBasicAuthHandler getChatTokenBasicAuthHandler(Pool pool, SQLDialect dbType) {
        return new TokenBasicAuthHandler(new ConversationTokenProvider());
    }
}
