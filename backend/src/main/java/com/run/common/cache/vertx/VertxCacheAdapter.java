package com.run.common.cache.vertx;

import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.Optional;

public class VertxCacheAdapter<K, V> {

    private final Vertx vertx;
    private final CacheStore<K, V> delegate;

    public VertxCacheAdapter(Vertx vertx, CacheStore<K, V> delegate) {
        this.vertx = vertx;
        this.delegate = delegate;
    }

    public Future<Optional<V>> get(K key) {
        return Future.fromCompletionStage(delegate.get(key), vertx.getOrCreateContext());
    }

    public Future<Void> put(K key, V value, CacheWriteOptions options) {
        return Future.fromCompletionStage(delegate.put(key, value, options), vertx.getOrCreateContext());
    }

    public Future<Void> evict(K key) {
        return Future.fromCompletionStage(delegate.evict(key), vertx.getOrCreateContext());
    }

    public Future<Void> clear() {
        return Future.fromCompletionStage(delegate.clear(), vertx.getOrCreateContext());
    }
}
