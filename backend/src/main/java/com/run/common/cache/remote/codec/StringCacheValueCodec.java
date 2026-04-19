package com.run.common.cache.remote.codec;

import com.run.common.cache.remote.CacheValueCodec;

public class StringCacheValueCodec implements CacheValueCodec<String> {

    @Override
    public String encode(String value) {
        return value;
    }

    @Override
    public String decode(String value) {
        return value;
    }
}
