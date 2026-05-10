package com.run.datasources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.cache.CacheWriteOptions;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * 简单缓存接口
 */
public interface SimpleCache {

    <T> CompletionStage<Optional<T>> get(String key, Class<T> type);

    <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference);

    CompletionStage<Void> set(String key, Object value, CacheWriteOptions options);

    CompletionStage<Long> delete(String key);

    CompletionStage<Boolean> exists(String key);

    CompletionStage<Boolean> expire(String key, long ttlMillis);

    CompletionStage<Void> close();

    default CompletionStage<Void> set(String key, Object value) {
        return set(key, value, CacheWriteOptions.DEFAULT);
    }
}
