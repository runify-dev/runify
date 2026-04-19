package com.run.common.cache.remote;

@FunctionalInterface
public interface CacheKeyEncoder<K> {

    String encode(K key);

    static CacheKeyEncoder<String> identity() {
        return key -> key;
    }
}
