package com.run.ai.openai;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lightweight SSE stream response. Subscribe once, keep the returned StreamSubscription, and cancel it when needed.
 */
public final class AsyncStreamResponse<T> {

    // 流式读取超时时间（秒），默认 60 秒
    private static final long STREAM_READ_TIMEOUT_SECONDS = 60;

    private final Call call;
    private final Executor executor;
    private final Function<String, T> decoder;
    private final CompletableFuture<Void> completeFuture = new CompletableFuture<>();
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private volatile StreamSubscription subscription;

    public AsyncStreamResponse(Call call, Executor executor, Function<String, T> decoder) {
        this.call = call;
        this.executor = executor;
        this.decoder = decoder;
    }

    public StreamSubscription subscribe(Consumer<T> consumer) {
        return subscribe(new Handler<>() {
            @Override
            public void onNext(T item) {
                consumer.accept(item);
            }
        });
    }

    public StreamSubscription subscribe(Handler<T> handler) {
        if (!subscribed.compareAndSet(false, true)) {
            throw new IllegalStateException("stream already subscribed");
        }

        StreamSubscription sub = new StreamSubscription(call, completeFuture);
        this.subscription = sub;
        handler.onSubscribe(sub);

        executor.execute(() -> runStream(handler, sub));
        return sub;
    }

    /**
     * Optional convenience method. Prefer keeping and cancelling StreamSubscription in business code.
     */
    public boolean cancel() {
        StreamSubscription sub = this.subscription;
        if (sub != null) {
            return sub.cancel();
        }
        call.cancel();
        return true;
    }

    public CompletableFuture<Void> onCompleteFuture() {
        return completeFuture;
    }

    private void runStream(Handler<T> handler, StreamSubscription sub) {
        Optional<Throwable> error = Optional.empty();

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() == null ? "" : response.body().string();
                throw new OpenAiException("OpenAI stream request failed: HTTP " + response.code() + ", body=" + body);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new OpenAiException("OpenAI stream response body is empty");
            }

            try (responseBody) {
                readSse(responseBody.source(), handler, sub);
            }
        } catch (Throwable e) {
            if (!sub.isCancelled() && !isOkHttpCancel(e)) {
                error = Optional.of(e);
            }
        } finally {
            finish(handler, sub, error);
        }
    }

    private void readSse(BufferedSource source, Handler<T> handler, StreamSubscription sub) throws IOException {
        StringBuilder dataBuffer = new StringBuilder();
        resetDeadline(source);

        while (!sub.isCancelled()) {
            String line = source.readUtf8Line();
            if (line == null) {
                dispatchEvent(dataBuffer, handler, sub);
                break;
            }

            if (line.isBlank()) {
                if (dispatchEvent(dataBuffer, handler, sub)) {
                    break;
                }
                continue;
            }

            // 跳过注释行（心跳）
            if (line.startsWith(":")) {
                continue;
            }

            if (line.startsWith("data:")) {
                if (dataBuffer.length() > 0) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(line.substring("data:".length()).trim());
            }

            resetDeadline(source);
        }
    }

    private void resetDeadline(BufferedSource source) {
        source.timeout().deadline(System.nanoTime()
                + TimeUnit.SECONDS.toNanos(STREAM_READ_TIMEOUT_SECONDS),
                TimeUnit.NANOSECONDS);
    }

    /**
     * @return true if stream should stop
     */
    private boolean dispatchEvent(StringBuilder dataBuffer, Handler<T> handler, StreamSubscription sub) {
        if (dataBuffer.length() == 0) {
            return false;
        }

        String data = dataBuffer.toString();
        dataBuffer.setLength(0);

        if ("[DONE]".equals(data)) {
            return true;
        }
        if (sub.isCancelled()) {
            return true;
        }

        T item = decoder.apply(data);
        if (item != null && !sub.isCancelled()) {
            handler.onNext(item);
        }
        return sub.isCancelled();
    }

    private void finish(Handler<T> handler, StreamSubscription sub, Optional<Throwable> error) {
        if (sub.isCancelled()) {
            safeCancel(handler);
            completeFuture.complete(null);
            return;
        }

        safeComplete(handler, error);
        if (error.isPresent()) {
            completeFuture.completeExceptionally(error.get());
        } else {
            completeFuture.complete(null);
        }
    }

    private static boolean isOkHttpCancel(Throwable e) {
        return e instanceof IOException && "Canceled".equalsIgnoreCase(e.getMessage());
    }

    private static <T> void safeCancel(Handler<T> handler) {
        try {
            handler.onCancel();
        } catch (Throwable ignored) {
        }
    }

    private static <T> void safeComplete(Handler<T> handler, Optional<Throwable> error) {
        try {
            handler.onComplete(error);
        } catch (Throwable ignored) {
        }
    }

    public interface Handler<T> {

        default void onSubscribe(StreamSubscription subscription) {
        }

        void onNext(T item);

        default void onComplete(Optional<Throwable> error) {
        }

        default void onCancel() {
        }
    }
}
