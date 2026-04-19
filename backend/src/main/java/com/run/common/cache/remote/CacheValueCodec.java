package com.run.common.cache.remote;

public interface CacheValueCodec<V> {

    String encode(V value);

    V decode(String value);
}
