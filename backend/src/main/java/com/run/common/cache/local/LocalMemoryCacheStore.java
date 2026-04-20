package com.run.common.cache.local;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.run.common.cache.CacheStore;
import com.run.common.cache.CacheWriteOptions;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMemoryCacheStore implements CacheStore {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final ConcurrentHashMap<String, Entry<Object>> kvStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, HashEntry> hashStorage = new ConcurrentHashMap<>();

    /* ----------------------------- KV ----------------------------- */

    @Override
    public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
        Entry<Object> entry = getAliveEntry(key);
        if (entry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(decodeLocal(entry.value(), type)));
    }

    @Override
    public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> typeReference) {
        Entry<Object> entry = getAliveEntry(key);
        if (entry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(decodeLocal(entry.value(), typeReference)));
    }

    @Override
    public CompletionStage<Void> set(String key, Object value, CacheWriteOptions options) {
        Objects.requireNonNull(key, "cache key must not be null");
        Objects.requireNonNull(value, "cache value must not be null");
        kvStorage.put(key, new Entry<>(value, toExpireAt(options)));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> delete(String key) {
        kvStorage.remove(key);
        hashStorage.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> clear() {
        kvStorage.clear();
        hashStorage.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Boolean> exists(String key) {
        return CompletableFuture.completedFuture(getAliveEntry(key) != null);
    }

    @Override
    public CompletionStage<Void> expire(String key, Duration ttl) {
        Entry<Object> entry = getAliveEntry(key);
        if (entry == null) {
            return CompletableFuture.completedFuture(null);
        }
        kvStorage.put(key, new Entry<>(entry.value(), toExpireAt(CacheWriteOptions.ofTtl(ttl))));
        return CompletableFuture.completedFuture(null);
    }

    /* ----------------------------- HASH ----------------------------- */

    @Override
    public <T> CompletionStage<Optional<T>> hget(String key, String field, Class<T> type) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Object value = hashEntry.values().get(field);
        return CompletableFuture.completedFuture(value == null
                ? Optional.empty()
                : Optional.ofNullable(decodeLocal(value, type)));
    }

    @Override
    public <T> CompletionStage<Optional<T>> hget(String key, String field, TypeReference<T> typeReference) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Object value = hashEntry.values().get(field);
        return CompletableFuture.completedFuture(value == null
                ? Optional.empty()
                : Optional.ofNullable(decodeLocal(value, typeReference)));
    }

    @Override
    public CompletionStage<Void> hset(String key, String field, Object value) {
        Objects.requireNonNull(key, "cache key must not be null");
        Objects.requireNonNull(field, "hash field must not be null");
        Objects.requireNonNull(value, "cache value must not be null");

        HashEntry current = getAliveHashEntry(key);
        ConcurrentHashMap<String, Object> values = current == null
                ? new ConcurrentHashMap<>()
                : current.values();
        long expireAt = current == null ? 0L : current.expireAtEpochMs();

        values.put(field, value);
        hashStorage.put(key, new HashEntry(values, expireAt));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> hset(String key, Map<String, ?> values) {
        Objects.requireNonNull(key, "cache key must not be null");
        if (values == null || values.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        HashEntry current = getAliveHashEntry(key);
        ConcurrentHashMap<String, Object> target = current == null
                ? new ConcurrentHashMap<>()
                : current.values();
        long expireAt = current == null ? 0L : current.expireAtEpochMs();

        values.forEach((field, value) -> {
            Objects.requireNonNull(field, "hash field must not be null");
            Objects.requireNonNull(value, "cache value must not be null");
            target.put(field, value);
        });

        hashStorage.put(key, new HashEntry(target, expireAt));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, Class<T> type) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Map<String, T> result = new LinkedHashMap<>();
        hashEntry.values().forEach((field, value) -> result.put(field, decodeLocal(value, type)));
        return CompletableFuture.completedFuture(Optional.of(result));
    }

    @Override
    public <T> CompletionStage<Optional<Map<String, T>>> hgetall(String key, TypeReference<T> typeReference) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        Map<String, T> result = new LinkedHashMap<>();
        hashEntry.values().forEach((field, value) -> result.put(field, decodeLocal(value, typeReference)));
        return CompletableFuture.completedFuture(Optional.of(result));
    }

    @Override
    public CompletionStage<Void> hdel(String key, String field) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry != null) {
            hashEntry.values().remove(field);
            if (hashEntry.values().isEmpty()) {
                hashStorage.remove(key, hashEntry);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> hclear(String key) {
        hashStorage.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Boolean> hexists(String key, String field) {
        HashEntry hashEntry = getAliveHashEntry(key);
        return CompletableFuture.completedFuture(hashEntry != null && hashEntry.values().containsKey(field));
    }

    @Override
    public CompletionStage<Void> hexpire(String key, Duration ttl) {
        HashEntry hashEntry = getAliveHashEntry(key);
        if (hashEntry == null) {
            return CompletableFuture.completedFuture(null);
        }
        hashStorage.put(key, new HashEntry(hashEntry.values(), toExpireAt(CacheWriteOptions.ofTtl(ttl))));
        return CompletableFuture.completedFuture(null);
    }

    /* ----------------------------- internal ----------------------------- */

    private Entry<Object> getAliveEntry(String key) {
        Entry<Object> entry = kvStorage.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            kvStorage.remove(key, entry);
            return null;
        }
        return entry;
    }

    private HashEntry getAliveHashEntry(String key) {
        HashEntry entry = hashStorage.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            hashStorage.remove(key, entry);
            return null;
        }
        return entry;
    }

    private long toExpireAt(CacheWriteOptions options) {
        if (options == null || !options.hasTtl()) {
            return 0L;
        }
        return System.currentTimeMillis() + options.ttl().toMillis();
    }

    private <T> T decodeLocal(Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type == String.class && value instanceof String str) {
            return type.cast(str);
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return MAPPER.convertValue(value, type);
    }

    @SuppressWarnings("unchecked")
    private <T> T decodeLocal(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return null;
        }

        JavaType javaType = MAPPER.getTypeFactory().constructType(typeReference);

        if (javaType.getRawClass() == String.class && value instanceof String str && !javaType.hasGenericTypes()) {
            return (T) str;
        }

        return MAPPER.convertValue(value, javaType);
    }

    private record Entry<V>(V value, long expireAtEpochMs) {
        boolean isExpired() {
            return expireAtEpochMs > 0 && System.currentTimeMillis() >= expireAtEpochMs;
        }
    }

    private record HashEntry(ConcurrentHashMap<String, Object> values, long expireAtEpochMs) {
        boolean isExpired() {
            return expireAtEpochMs > 0 && System.currentTimeMillis() >= expireAtEpochMs;
        }
    }
}