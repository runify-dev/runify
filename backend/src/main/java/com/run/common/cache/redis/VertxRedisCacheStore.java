package com.run.common.cache.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class VertxRedisCacheStore implements CacheStore {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final RedisAPI redisAPI;
    private final String namespace;
    private final int scanCount;

    public VertxRedisCacheStore(RedisAPI redisAPI, String namespace) {
        this(redisAPI, namespace, 500);
    }

    public VertxRedisCacheStore(RedisAPI redisAPI, String namespace, int scanCount) {
        this.redisAPI = redisAPI;
        this.namespace = namespace;
        this.scanCount = scanCount;
    }

    /* ----------------------------- KV ----------------------------- */

    @Override
    public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
        return getEncodedValue(key).thenApply(opt -> opt.map(value -> decodeRedis(value, type)));
    }

    @Override
    public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference) {
        return getEncodedValue(key).thenApply(opt -> opt.map(value -> decodeRedis(value, typeReference)));
    }

    @Override
    public CompletionStage<Void> set(String key, Object value, CacheWriteOptions options) {
        Objects.requireNonNull(key, "cache key must not be null");
        Objects.requireNonNull(value, "cache value must not be null");

        String storageKey = storageKey(key);
        String encodedValue = encodeRedis(value);

        if (options == null || !options.hasTtl()) {
            return toStage(redisAPI.set(List.of(storageKey, encodedValue))).thenApply(ignore -> null);
        }

        return toStage(redisAPI.set(List.of(
                storageKey,
                encodedValue,
                "PX",
                String.valueOf(options.ttl().toMillis())
        ))).thenApply(ignore -> null);
    }

    @Override
    public CompletionStage<Void> delete(String key) {
        return toStage(redisAPI.del(List.of(storageKey(key)))).thenApply(ignore -> null);
    }

    @Override
    public CompletionStage<Void> clear() {
        if (namespace == null || namespace.isBlank()) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("clear() requires a non-blank namespace"));
            return failed;
        }
        return scanAndDelete("0", namespace + ":*");
    }

    @Override
    public CompletionStage<Boolean> exists(String key) {
        return toStage(redisAPI.exists(List.of(storageKey(key))))
                .thenApply(resp -> resp != null && Long.parseLong(resp.toString()) > 0);
    }

    @Override
    public CompletionStage<Void> expire(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return CompletableFuture.completedFuture(null);
        }
        return toStage(redisAPI.pexpire(List.of(storageKey(key), String.valueOf(ttl.toMillis()))))
                .thenApply(ignore -> null);
    }

    /* ----------------------------- HASH ----------------------------- */

    @Override
    public <T> CompletionStage<Optional<T>> hget(String key, String field, Class<T> type) {
        return getEncodedHashValue(key, field).thenApply(opt -> opt.map(value -> decodeRedis(value, type)));
    }

    @Override
    public <T> CompletionStage<Optional<T>> hget(String key, String field, TypeReference<T> typeReference) {
        return getEncodedHashValue(key, field).thenApply(opt -> opt.map(value -> decodeRedis(value, typeReference)));
    }

    @Override
    public CompletionStage<Void> hset(String key, String field, Object value) {
        Objects.requireNonNull(key, "cache key must not be null");
        Objects.requireNonNull(field, "hash field must not be null");
        Objects.requireNonNull(value, "cache value must not be null");

        return toStage(redisAPI.hset(List.of(
                storageKey(key),
                field,
                encodeRedis(value)
        ))).thenApply(ignore -> null);
    }

    @Override
    public CompletionStage<Void> hset(String key, Map<String, ?> values) {
        Objects.requireNonNull(key, "cache key must not be null");
        if (values == null || values.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> args = new ArrayList<>();
        args.add(storageKey(key));

        values.forEach((field, value) -> {
            Objects.requireNonNull(field, "hash field must not be null");
            Objects.requireNonNull(value, "cache value must not be null");
            args.add(field);
            args.add(encodeRedis(value));
        });

        return toStage(redisAPI.hset(args)).thenApply(ignore -> null);
    }

    @Override
    public <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, Class<T> type) {
        return getEncodedHashAll(key).thenApply(opt -> {
            if (opt.isEmpty()) {
                return Optional.empty();
            }

            Map<String, T> result = new LinkedHashMap<>();
            opt.get().forEach((field, value) -> result.put(field, decodeRedis(value, type)));
            return Optional.of(result);
        });
    }

    @Override
    public <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, TypeReference<T> typeReference) {
        return getEncodedHashAll(key).thenApply(opt -> {
            if (opt.isEmpty()) {
                return Optional.empty();
            }

            Map<String, T> result = new LinkedHashMap<>();
            opt.get().forEach((field, value) -> result.put(field, decodeRedis(value, typeReference)));
            return Optional.of(result);
        });
    }

    @Override
    public CompletionStage<Void> hdel(String key, String field) {
        return toStage(redisAPI.hdel(List.of(storageKey(key), field)))
                .thenApply(ignore -> null);
    }

    @Override
    public CompletionStage<Void> hclear(String key) {
        return toStage(redisAPI.del(List.of(storageKey(key)))).thenApply(ignore -> null);
    }

    @Override
    public CompletionStage<Boolean> hexists(String key, String field) {
        return toStage(redisAPI.hexists(storageKey(key), field))
                .thenApply(resp -> resp != null && Long.parseLong(resp.toString()) > 0);
    }

    @Override
    public CompletionStage<Void> hexpire(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return CompletableFuture.completedFuture(null);
        }
        return toStage(redisAPI.pexpire(List.of(storageKey(key), String.valueOf(ttl.toMillis()))))
                .thenApply(ignore -> null);
    }

    /* ----------------------------- internal ----------------------------- */

    private CompletionStage<Optional<String>> getEncodedValue(String key) {
        String storageKey = storageKey(key);
        return toStage(redisAPI.get(storageKey))
                .thenApply(resp -> resp == null ? Optional.empty() : Optional.of(resp.toString()));
    }

    private CompletionStage<Optional<String>> getEncodedHashValue(String key, String field) {
        return toStage(redisAPI.hget(storageKey(key), field))
                .thenApply(resp -> resp == null ? Optional.empty() : Optional.of(resp.toString()));
    }

    private CompletionStage<Optional<Map<String, String>>> getEncodedHashAll(String key) {
        return toStage(redisAPI.hgetall(storageKey(key))).thenApply(resp -> {
            if (resp == null || resp.size() == 0) {
                return Optional.empty();
            }

            Map<String, String> result = new LinkedHashMap<>();
            for (int i = 0; i < resp.size(); i += 2) {
                result.put(resp.get(i).toString(), resp.get(i + 1).toString());
            }
            return Optional.of(result);
        });
    }

    private CompletionStage<Void> scanAndDelete(String cursor, String pattern) {
        List<String> args = List.of(cursor, "MATCH", pattern, "COUNT", String.valueOf(scanCount));
        return toStage(redisAPI.scan(args)).thenCompose(resp -> {
            String nextCursor = resp.get(0).toString();
            Response keys = resp.get(1);

            CompletionStage<Void> deleteStage;
            if (keys != null && keys.size() > 0) {
                List<String> deleteArgs = new ArrayList<>();
                for (int i = 0; i < keys.size(); i++) {
                    deleteArgs.add(keys.get(i).toString());
                }
                deleteStage = toStage(redisAPI.del(deleteArgs)).thenApply(ignore -> null);
            } else {
                deleteStage = CompletableFuture.completedFuture(null);
            }

            if ("0".equals(nextCursor)) {
                return deleteStage;
            }
            return deleteStage.thenCompose(v -> scanAndDelete(nextCursor, pattern));
        });
    }

    private String storageKey(String key) {
        if (namespace == null || namespace.isBlank()) {
            return key;
        }
        return namespace + ":" + key;
    }

    private String encodeRedis(Object value) {
        if (value instanceof String str) {
            return str;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize cache value", e);
        }
    }

    private <T> T decodeRedis(String value, Class<T> type) {
        if (type == String.class) {
            return type.cast(value);
        }

        if (type == Object.class) {
            try {
                return type.cast(MAPPER.readValue(value, Object.class));
            } catch (Exception ignore) {
                return type.cast(value);
            }
        }

        try {
            return MAPPER.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize cache value", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T decodeRedis(String value, TypeReference<T> typeReference) {
        JavaType javaType = MAPPER.getTypeFactory().constructType(typeReference);

        if (javaType.getRawClass() == String.class && !javaType.hasGenericTypes()) {
            return (T) value;
        }

        if (javaType.getRawClass() == Object.class && !javaType.hasGenericTypes()) {
            try {
                return MAPPER.readValue(value, javaType);
            } catch (Exception ignore) {
                return (T) value;
            }
        }

        try {
            return MAPPER.readValue(value, javaType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize cache value", e);
        }
    }

    private CompletionStage<Response> toStage(Future<Response> future) {
        Promise<Response> promise = Promise.promise();
        future.onSuccess(promise::complete).onFailure(promise::fail);
        return promise.future().toCompletionStage();
    }
}