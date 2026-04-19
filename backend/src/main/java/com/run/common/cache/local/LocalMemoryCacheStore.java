package com.run.common.cache.local;

import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMemoryCacheStore<K, V> implements CacheStore<K, V> {

    private final String name;
    private final ConcurrentHashMap<K, Entry<V>> storage = new ConcurrentHashMap<>();

    public LocalMemoryCacheStore(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletionStage<Optional<V>> get(K key) {
        Entry<V> entry = storage.get(key);
        if (entry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        if (entry.isExpired()) {
            storage.remove(key, entry);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.completedFuture(Optional.of(entry.value()));
    }

    @Override
    public CompletionStage<Void> put(K key, V value, CacheWriteOptions options) {
        long expireAt = 0L;
        if (options != null && options.hasTtl()) {
            Duration ttl = options.ttl();
            expireAt = System.currentTimeMillis() + ttl.toMillis();
        }
        storage.put(key, new Entry<>(value, expireAt));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> evict(K key) {
        storage.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> clear() {
        storage.clear();
        return CompletableFuture.completedFuture(null);
    }

    private record Entry<V>(V value, long expireAtEpochMs) {
        boolean isExpired() {
            return expireAtEpochMs > 0 && System.currentTimeMillis() >= expireAtEpochMs;
        }
    }
}
