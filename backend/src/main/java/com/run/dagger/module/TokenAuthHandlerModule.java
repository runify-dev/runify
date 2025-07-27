package com.run.dagger.module;



import com.run.auth.TokenBasicAuthHandler;
import com.run.common.constants.DatabaseType;
import dagger.Module;
import dagger.Provides;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class TokenAuthHandlerModule   {
    @Inject
    @Singleton
    @Provides
    public TokenBasicAuthHandler getTokenBasicAuthHandler(Pool pool, DatabaseType dbType) {
        return new TokenBasicAuthHandler(pool, dbType);
    }


}
