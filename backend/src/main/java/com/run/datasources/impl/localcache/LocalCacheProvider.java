package com.run.datasources.impl.localcache;

import com.run.common.cache.CacheWriteOptions;
import com.run.common.cache.local.LocalMemoryCacheStore;
import com.run.common.util.CommonUtils;
import com.run.datasources.*;
import com.run.datasources.impl.localcache.credential.LocalCacheCredential;
import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class LocalCacheProvider implements CacheProvider {

    private static final DatasourceProviderInfo info;
    private static final LocalCacheCredential credential;

    static {
        String icon = CommonUtils.getFileContent("com/run/datasources/impl/localcache/icon/localcache.svg");
        info = new DatasourceProviderInfo("local_cache", "本地缓存", icon, DataSourceType.CACHE);
        credential = new LocalCacheCredential();
    }

    @Override
    public DatasourceProviderInfo info() {
        return info;
    }

    @Override
    public BaseDatasourceCredential getCredential() {
        return credential;
    }

    @Override
    public SimpleCache createCache(Datasource datasource, Vertx vertx) {
        return new LocalSimpleCache();
    }

    @Override
    public Future<Boolean> validate(Datasource datasource, Vertx vertx) {
        return Future.succeededFuture(true);
    }

    private static class LocalSimpleCache implements SimpleCache {

        private final LocalMemoryCacheStore store = new LocalMemoryCacheStore();

        @Override
        public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
            return store.get(key, type);
        }

        @Override
        public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference) {
            return store.get(key, typeReference);
        }

        @Override
        public CompletionStage<Void> set(String key, Object value, CacheWriteOptions options) {
            return store.set(key, value, options);
        }

        @Override
        public CompletionStage<Long> delete(String key) {
            return store.delete(key).thenApply(_ -> 1L);
        }

        @Override
        public CompletionStage<Boolean> exists(String key) {
            return store.exists(key);
        }

        @Override
        public CompletionStage<Boolean> expire(String key, long ttlMillis) {
            return store.expire(key, Duration.ofMillis(ttlMillis)).thenApply(_ -> true);
        }

        @Override
        public CompletionStage<Void> close() {
            store.clear();
            return CompletableFuture.completedFuture(null);
        }
    }
}
