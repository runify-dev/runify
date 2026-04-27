package com.run.common.queue;

import java.util.function.Consumer;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  22:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface MessageQueue<M> {

    void consumer(String eventId, String consumerId, Long index, Consumer<M> onNext, Runnable onComplete);

    void complete(String eventId);

    void publish(String eventId, Long index, M message);

    void create(String eventId);

    void delete(String eventId);

    void exists(String eventId, Consumer<Boolean> callback);
}
