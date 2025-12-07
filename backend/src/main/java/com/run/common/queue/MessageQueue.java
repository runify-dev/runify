package com.run.common.queue;

import java.util.function.Consumer;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  22:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface MessageQueue<M> {

    void consume(String eventId, String consumerId, Consumer<M> call);

    void publish(String eventId, M message);

    void create(String eventId);

    void delete(String eventId);
}
