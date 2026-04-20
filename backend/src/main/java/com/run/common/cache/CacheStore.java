package com.run.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface CacheStore {

    /* ----------------------------- KV ----------------------------- */

    <T> CompletionStage<Optional<T>> get(String key, Class<T> type);

    <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference);

    CompletionStage<Void> set(String key, Object value, CacheWriteOptions options);

    CompletionStage<Void> delete(String key);

    CompletionStage<Void> clear();

    CompletionStage<Boolean> exists(String key);

    CompletionStage<Void> expire(String key, Duration ttl);

    default CompletionStage<Void> set(String key, Object value) {
        return set(key, value, CacheWriteOptions.DEFAULT);
    }

    default <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, Class<T> type) {
        CompletionStage<Map<String, T>> stage = CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (String key : keys) {
            stage = stage.thenCompose(result ->
                    get(key, type).thenApply(opt -> {
                        opt.ifPresent(value -> result.put(key, value));
                        return result;
                    })
            );
        }
        return stage;
    }

    default <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, TypeReference<T> typeReference) {
        CompletionStage<Map<String, T>> stage = CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (String key : keys) {
            stage = stage.thenCompose(result ->
                    get(key, typeReference).thenApply(opt -> {
                        opt.ifPresent(value -> result.put(key, value));
                        return result;
                    })
            );
        }
        return stage;
    }

    default CompletionStage<Void> setAll(Map<String, ?> values, CacheWriteOptions options) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            stage = stage.thenCompose(v -> set(entry.getKey(), entry.getValue(), options));
        }
        return stage;
    }

    default CompletionStage<Void> setAll(Map<String, ?> values) {
        return setAll(values, CacheWriteOptions.DEFAULT);
    }

    default CompletionStage<Void> deleteAll(Collection<String> keys) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (String key : keys) {
            stage = stage.thenCompose(v -> delete(key));
        }
        return stage;
    }

    /* ----------------------------- HASH ----------------------------- */

    <T> CompletionStage<Optional<T>> hget(String key, String field, Class<T> type);

    <T> CompletionStage<Optional<T>> hget(String key, String field, TypeReference<T> typeReference);

    CompletionStage<Void> hset(String key, String field, Object value);

    CompletionStage<Void> hset(String key, Map<String, ?> values);

    <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, Class<T> type);

    <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, TypeReference<T> typeReference);

    CompletionStage<Void> hdel(String key, String field);

    CompletionStage<Void> hclear(String key);

    CompletionStage<Boolean> hexists(String key, String field);

    CompletionStage<Void> hexpire(String key, Duration ttl);

    default <T> CompletionStage<Map<String, T>> hmget(String key, Collection<String> fields, Class<T> type) {
        return hgetall(key, type).thenApply(opt -> {
            Map<String, T> result = new LinkedHashMap<>();
            if (opt.isEmpty()) {
                return result;
            }
            Map<String, T> all = opt.get();
            for (String field : fields) {
                T value = all.get(field);
                if (value != null) {
                    result.put(field, value);
                }
            }
            return result;
        });
    }

    default <T> CompletionStage<Map<String, T>> hmget(String key, Collection<String> fields, TypeReference<T> typeReference) {
        return hgetall(key, typeReference).thenApply(opt -> {
            Map<String, T> result = new LinkedHashMap<>();
            if (opt.isEmpty()) {
                return result;
            }
            Map<String, T> all = opt.get();
            for (String field : fields) {
                T value = all.get(field);
                if (value != null) {
                    result.put(field, value);
                }
            }
            return result;
        });
    }
}