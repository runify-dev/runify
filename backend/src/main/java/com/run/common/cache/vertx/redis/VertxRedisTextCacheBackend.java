package com.run.common.cache.vertx.redis;

import com.run.common.cache.remote.RemoteTextCacheBackend;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class VertxRedisTextCacheBackend implements RemoteTextCacheBackend {

    private final RedisAPI redisAPI;
    private final int scanCount;

    public VertxRedisTextCacheBackend(RedisAPI redisAPI) {
        this(redisAPI, 500);
    }

    public VertxRedisTextCacheBackend(RedisAPI redisAPI, int scanCount) {
        this.redisAPI = redisAPI;
        this.scanCount = scanCount;
    }

    @Override
    public CompletionStage<Optional<String>> get(String key) {
        return toStage(redisAPI.get(key))
                .thenApply(resp -> resp == null ? Optional.empty() : Optional.of(resp.toString()));
    }

    @Override
    public CompletionStage<Void> set(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return toStage(redisAPI.set(List.of(key, value))).thenApply(resp -> null);
        }
        return toStage(redisAPI.set(List.of(key, value, "PX", String.valueOf(ttl.toMillis()))))
                .thenApply(resp -> null);
    }

    @Override
    public CompletionStage<Void> delete(String key) {
        return toStage(redisAPI.del(List.of(key))).thenApply(resp -> null);
    }

    @Override
    public CompletionStage<Void> clearByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("prefix must not be blank"));
            return failed;
        }
        return scanAndDelete("0", prefix + "*");
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

    public CompletionStage<Optional<String>> hget(String key, String field) {
        return toStage(redisAPI.hget(key, field))
                .thenApply(resp -> resp == null ? Optional.empty() : Optional.of(resp.toString()));
    }

    public CompletionStage<Void> hset(String key, String field, String value) {
        return toStage(redisAPI.hset(List.of(key, field, value))).thenApply(resp -> null);
    }

    public CompletionStage<Void> hset(String key, java.util.Map<String, String> values) {
        List<String> args = new ArrayList<>();
        args.add(key);
        values.forEach((field, value) -> {
            args.add(field);
            args.add(value);
        });
        return toStage(redisAPI.hset(args)).thenApply(resp -> null);
    }

    public CompletionStage<Optional<java.util.Map<String, String>>> hgetall(String key) {
        return toStage(redisAPI.hgetall(key)).thenApply(resp -> {
            if (resp == null || resp.size() == 0) {
                return Optional.empty();
            }
            java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
            for (int i = 0; i < resp.size(); i += 2) {
                result.put(resp.get(i).toString(), resp.get(i + 1).toString());
            }
            return Optional.of(result);
        });
    }

    public CompletionStage<Void> hdel(String key, String field) {
        return toStage(redisAPI.hdel(List.of(key, field))).thenApply(resp -> null);
    }

    public CompletionStage<Void> delKey(String key) {
        return toStage(redisAPI.del(List.of(key))).thenApply(resp -> null);
    }

    public CompletionStage<Void> pexpire(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return CompletableFuture.completedFuture(null);
        }
        return toStage(redisAPI.pexpire(List.of(key, String.valueOf(ttl.toMillis()))))
                .thenApply(resp -> null);
    }

    private CompletionStage<Response> toStage(Future<Response> future) {
        Promise<Response> promise = Promise.promise();
        future.onSuccess(promise::complete).onFailure(promise::fail);
        return promise.future().toCompletionStage();
    }
}
