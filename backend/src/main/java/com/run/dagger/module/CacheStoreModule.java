package com.run.dagger.module;


import com.run.common.cache.CacheStore;
import com.run.common.cache.local.LocalMemoryCacheStore;
import com.run.common.cache.redis.VertxRedisCacheStore;
import com.run.common.config.AppConfig;
import com.run.common.config.Cache;
import com.run.common.config.CacheType;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.impl.RedisAPIImpl;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  01:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class CacheStoreModule {


    @Inject
    @Singleton
    @Provides
    public CacheStore cacheStore(AppConfig config, Vertx vertx) {
        Cache cache = config.getCache();
        if (cache == null || cache.getType() == CacheType.LOCAL) {
            return new LocalMemoryCacheStore();
        } else {
            return new VertxRedisCacheStore(new RedisAPIImpl(Redis.createClient(vertx,
                    new RedisOptions()
                            .setConnectionString(cache.getConnectionString())
                            .setPassword(cache.getPassword()))), "run");

        }

    }
}
