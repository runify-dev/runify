package com.run.datasources.impl.redis;

import com.run.common.util.CommonUtils;
import com.run.datasources.*;
import com.run.datasources.impl.redis.credential.RedisCredential;
import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.impl.RedisAPIImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.cache.CacheWriteOptions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Redis 数据源供应商实现
 */
public class RedisProvider implements CacheProvider {

    private static final DatasourceProviderInfo info;
    private static final RedisCredential credential;

    static {
        String icon = CommonUtils.getFileContent("com/run/datasources/impl/redis/icon/redis.svg");
        info = new DatasourceProviderInfo("redis", "Redis", icon, DataSourceType.CACHE);
        credential = new RedisCredential();
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
        JsonObject meta = datasource.decrypt();
        String host = meta.getString("host", "localhost");
        int port = meta.getInteger("port", 6379);
        String password = meta.getString("password", "");
        int database = meta.getInteger("database", 0);

        String connectionString = "redis://" + host + ":" + port + "/" + database;
        RedisOptions options = new RedisOptions().setConnectionString(connectionString);
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        RedisAPI redisAPI = new RedisAPIImpl(Redis.createClient(vertx, options));
        return new RedisSimpleCache(redisAPI);
    }

    @Override
    public Future<Boolean> validate(Datasource datasource, Vertx vertx) {
        JsonObject meta = datasource.decrypt();
        String host = meta.getString("host", "localhost");
        int port = meta.getInteger("port", 6379);
        String password = meta.getString("password", "");

        String connectionString = "redis://" + host + ":" + port;
        RedisOptions options = new RedisOptions().setConnectionString(connectionString);
        if (password != null && !password.isEmpty()) {
            options.setPassword(password);
        }
        RedisAPI redisAPI = new RedisAPIImpl(Redis.createClient(vertx, options));
        return redisAPI.ping(List.of())
                .compose(_ -> Future.succeededFuture(true))
                .onComplete(_ -> redisAPI.close());
    }

    private static class RedisSimpleCache implements SimpleCache {

        private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
        private final RedisAPI redis;

        RedisSimpleCache(RedisAPI redis) {
            this.redis = redis;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
            return redis.get(key).map(resp -> {
                if (resp == null) {
                    return Optional.<T>empty();
                }
                String value = resp.toString();
                if (type == String.class) {
                    return Optional.of((T) value);
                }
                try {
                    return Optional.of(MAPPER.readValue(value, type));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to deserialize cache value", e);
                }
            }).toCompletionStage();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference) {
            return redis.get(key).map(resp -> {
                if (resp == null) {
                    return Optional.<T>empty();
                }
                String value = resp.toString();
                JavaType javaType = MAPPER.getTypeFactory().constructType(typeReference);
                if (javaType.getRawClass() == String.class && !javaType.hasGenericTypes()) {
                    return Optional.of((T) value);
                }
                try {
                    T result = MAPPER.readValue(value, javaType);
                    return Optional.of(result);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to deserialize cache value", e);
                }
            }).toCompletionStage();
        }

        @Override
        public CompletionStage<Void> set(String key, Object value, CacheWriteOptions options) {
            String encoded = encode(value);
            if (options != null && options.hasTtl()) {
                return redis.set(List.of(key, encoded, "PX", String.valueOf(options.ttl().toMillis())))
                        .map(resp -> (Void) null).toCompletionStage();
            }
            return redis.set(List.of(key, encoded)).map(resp -> (Void) null).toCompletionStage();
        }

        private String encode(Object value) {
            if (value instanceof String str) {
                return str;
            }
            try {
                return MAPPER.writeValueAsString(value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize cache value", e);
            }
        }

        @Override
        public CompletionStage<Long> delete(String key) {
            return redis.del(List.of(key)).map(resp -> resp.toLong()).toCompletionStage();
        }

        @Override
        public CompletionStage<Boolean> exists(String key) {
            return redis.exists(List.of(key)).map(resp -> resp.toLong() > 0).toCompletionStage();
        }

        @Override
        public CompletionStage<Boolean> expire(String key, long ttlMillis) {
            return redis.pexpire(List.of(key, String.valueOf(ttlMillis))).map(resp -> resp.toLong() == 1).toCompletionStage();
        }

        @Override
        public CompletionStage<Void> close() {
            redis.close();
            return CompletableFuture.completedFuture(null);
        }
    }
}
