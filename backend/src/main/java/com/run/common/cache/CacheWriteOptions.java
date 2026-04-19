package com.run.common.cache;

import java.time.Duration;

public record CacheWriteOptions(Duration ttl) {

    public static final CacheWriteOptions DEFAULT = new CacheWriteOptions(null);

    public static CacheWriteOptions ofTtl(Duration ttl) {
        return new CacheWriteOptions(ttl);
    }

    public boolean hasTtl() {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }
}
