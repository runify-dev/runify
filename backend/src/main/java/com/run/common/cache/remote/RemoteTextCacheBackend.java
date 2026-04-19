package com.run.common.cache.remote;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface RemoteTextCacheBackend {

    CompletionStage<Optional<String>> get(String key);

    CompletionStage<Void> set(String key, String value, Duration ttl);

    CompletionStage<Void> delete(String key);

    CompletionStage<Void> clearByPrefix(String prefix);
}
