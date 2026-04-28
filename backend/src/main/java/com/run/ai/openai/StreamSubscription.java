package com.run.ai.openai;

import okhttp3.Call;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents one active streaming subscription. Call cancel/close to disconnect the SSE HTTP connection.
 */
public final class StreamSubscription implements AutoCloseable {

    private final Call call;
    private final CompletableFuture<Void> completeFuture;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public StreamSubscription(Call call, CompletableFuture<Void> completeFuture) {
        this.call = call;
        this.completeFuture = completeFuture;
    }

    /**
     * Cancel the underlying OkHttp call. This closes the network stream, not just the Java callback.
     */
    public boolean cancel() {
        if (cancelled.compareAndSet(false, true)) {
            call.cancel();
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return cancelled.get() || call.isCanceled();
    }

    public CompletableFuture<Void> onCompleteFuture() {
        return completeFuture;
    }

    @Override
    public void close() {
        cancel();
    }
}
