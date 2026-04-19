package com.run.common.cache.remote;

import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class RemoteTextCacheStore<K, V> implements CacheStore<K, V> {

    private final String name;
    private final String namespace;
    private final RemoteTextCacheBackend backend;
    private final CacheKeyEncoder<K> keyEncoder;
    private final CacheValueCodec<V> codec;

    public RemoteTextCacheStore(String name,
                                String namespace,
                                RemoteTextCacheBackend backend,
                                CacheKeyEncoder<K> keyEncoder,
                                CacheValueCodec<V> codec) {
        this.name = name;
        this.namespace = namespace;
        this.backend = backend;
        this.keyEncoder = keyEncoder;
        this.codec = codec;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletionStage<Optional<V>> get(K key) {
        String storageKey = storageKey(key);
        return backend.get(storageKey).thenApply(opt -> opt.map(codec::decode));
    }

    @Override
    public CompletionStage<Void> put(K key, V value, CacheWriteOptions options) {
        String storageKey = storageKey(key);
        String encodedValue = codec.encode(value);
        Duration ttl = options != null && options.hasTtl() ? options.ttl() : null;
        return backend.set(storageKey, encodedValue, ttl);
    }

    @Override
    public CompletionStage<Void> evict(K key) {
        return backend.delete(storageKey(key));
    }

    @Override
    public CompletionStage<Void> clear() {
        String prefix = namespace == null || namespace.isBlank() ? "" : namespace + ":";
        return backend.clearByPrefix(prefix);
    }

    private String storageKey(K key) {
        String encodedKey = keyEncoder.encode(key);
        if (namespace == null || namespace.isBlank()) {
            return encodedKey;
        }
        return namespace + ":" + encodedKey;
    }
}
