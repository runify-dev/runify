package com.run.common.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface CacheStore<K, V> {

    String name();

    CompletionStage<Optional<V>> get(K key);

    CompletionStage<Void> put(K key, V value, CacheWriteOptions options);

    CompletionStage<Void> evict(K key);

    CompletionStage<Void> clear();

    default CompletionStage<Map<K, V>> getAll(Collection<K> keys) {
        CompletionStage<Map<K, V>> stage = CompletableFuture.completedFuture(new LinkedHashMap<>());
        for (K key : keys) {
            stage = stage.thenCompose(result ->
                    get(key).thenApply(opt -> {
                        opt.ifPresent(value -> result.put(key, value));
                        return result;
                    })
            );
        }
        return stage;
    }

    default CompletionStage<Void> putAll(Map<K, V> values, CacheWriteOptions options) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (Map.Entry<K, V> entry : values.entrySet()) {
            stage = stage.thenCompose(v -> put(entry.getKey(), entry.getValue(), options));
        }
        return stage;
    }

    default CompletionStage<Void> evictAll(Collection<K> keys) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (K key : keys) {
            stage = stage.thenCompose(v -> evict(key));
        }
        return stage;
    }
}
