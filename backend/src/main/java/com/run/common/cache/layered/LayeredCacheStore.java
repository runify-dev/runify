package com.run.common.cache.layered;

import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class LayeredCacheStore<K, V> implements CacheStore<K, V> {

    private final String name;
    private final List<CacheStore<K, V>> layers;
    private final CacheWriteOptions backfillOptions;

    public LayeredCacheStore(String name, List<CacheStore<K, V>> layers) {
        this(name, layers, CacheWriteOptions.DEFAULT);
    }

    public LayeredCacheStore(String name,
                             List<CacheStore<K, V>> layers,
                             CacheWriteOptions backfillOptions) {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException("layers must not be empty");
        }
        this.name = name;
        this.layers = List.copyOf(layers);
        this.backfillOptions = backfillOptions == null ? CacheWriteOptions.DEFAULT : backfillOptions;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletionStage<Optional<V>> get(K key) {
        return getFromLayer(key, 0, new ArrayList<>());
    }

    private CompletionStage<Optional<V>> getFromLayer(K key,
                                                      int index,
                                                      List<CacheStore<K, V>> missedLayers) {
        if (index >= layers.size()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        CacheStore<K, V> current = layers.get(index);
        return current.get(key).thenCompose(opt -> {
            if (opt.isPresent()) {
                V value = opt.get();
                return backfill(key, value, missedLayers).thenApply(v -> opt);
            }

            missedLayers.add(current);
            return getFromLayer(key, index + 1, missedLayers);
        });
    }

    private CompletionStage<Void> backfill(K key, V value, List<CacheStore<K, V>> missedLayers) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (CacheStore<K, V> layer : missedLayers) {
            stage = stage.thenCompose(v -> layer.put(key, value, backfillOptions));
        }
        return stage;
    }

    @Override
    public CompletionStage<Void> put(K key, V value, CacheWriteOptions options) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (CacheStore<K, V> layer : layers) {
            stage = stage.thenCompose(v -> layer.put(key, value, options));
        }
        return stage;
    }

    @Override
    public CompletionStage<Void> evict(K key) {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (CacheStore<K, V> layer : layers) {
            stage = stage.thenCompose(v -> layer.evict(key));
        }
        return stage;
    }

    @Override
    public CompletionStage<Void> clear() {
        CompletionStage<Void> stage = CompletableFuture.completedFuture(null);
        for (CacheStore<K, V> layer : layers) {
            stage = stage.thenCompose(v -> layer.clear());
        }
        return stage;
    }
}
