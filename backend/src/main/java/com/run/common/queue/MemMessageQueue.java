package com.run.common.queue;

import io.vertx.core.Vertx;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  22:41}
 * {@code @Version 1.0}
 * {@code @注释: 内存消息队列}
 */
public class MemMessageQueue implements MessageQueue<String> {

    /**
     * 运行中默认保留 10 分钟。
     * 每次 publish 都会续期。
     */
    private static final long ACTIVE_TTL_MS = TimeUnit.MINUTES.toMillis(10);

    /**
     * complete 后保留 1 分钟。
     */
    private static final long COMPLETE_TTL_MS = TimeUnit.MINUTES.toMillis(1);

    /**
     * 本地清理间隔。
     */
    private static final long CLEAN_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);

    /**
     * 没有新消息时的轮询间隔。
     */
    private static final long POLL_INTERVAL_MS = 200;

    /**
     * onNext 异常后的重试间隔。
     */
    private static final long RETRY_INTERVAL_MS = 1_000;

    private final Vertx vertx;

    private final ConcurrentMap<String, Instance> eventMap = new ConcurrentHashMap<>();

    /**
     * 同一个 JVM 内，同一个 eventId + consumerId 新连接替换旧连接。
     */
    private final ConcurrentMap<String, ActiveConsumer> activeConsumerMap = new ConcurrentHashMap<>();

    private final AtomicLong tokenGenerator = new AtomicLong(0);

    private final long cleanerTimerId;

    public MemMessageQueue(Vertx vertx) {
        this.vertx = Objects.requireNonNull(vertx, "vertx can not be null");

        this.cleanerTimerId = vertx.setPeriodic(
                CLEAN_INTERVAL_MS,
                ignored -> cleanExpiredEvents()
        );
    }

    @Override
    public void consumer(String eventId,
                         String consumerId,
                         Long index,
                         Consumer<String> onNext,
                         Runnable onComplete) {
        requireNotBlank("eventId", eventId);
        requireNotBlank("consumerId", consumerId);
        Objects.requireNonNull(onNext, "onNext can not be null");
        Objects.requireNonNull(onComplete, "onComplete can not be null");

        long afterIndex = index == null ? 0L : index;

        if (afterIndex < 0) {
            throw new IllegalArgumentException("index can not be negative");
        }

        Instance instance = eventMap.get(eventId);

        /*
         * 和 Redis 版保持一致：
         * eventId 不存在或者已经过期，直接 onComplete。
         */
        if (instance == null) {
            onComplete.run();
            return;
        }

        String activeKey = activeConsumerKey(eventId, consumerId);

        ActiveConsumer session = new ActiveConsumer(
                activeKey,
                tokenGenerator.incrementAndGet(),
                onComplete
        );

        ActiveConsumer old = activeConsumerMap.put(activeKey, session);
        if (old != null) {
            old.complete();
        }

        localLoop(eventId, consumerId, session, afterIndex, onNext);
    }

    @Override
    public void publish(String eventId, Long index, String message) {
        requireNotBlank("eventId", eventId);
        Objects.requireNonNull(index, "index can not be null");
        Objects.requireNonNull(message, "message can not be null");

        if (index <= 0) {
            throw new IllegalArgumentException("index must be greater than 0");
        }

        Instance instance = eventMap.get(eventId);
        if (instance == null) {
            throw new IllegalStateException("EventId not found: " + eventId + ", please call create() first");
        }

        instance.publish(index, message);
    }

    @Override
    public void create(String eventId) {
        requireNotBlank("eventId", eventId);

        Instance old = eventMap.put(eventId, new Instance());

        if (old != null) {
            old.forceComplete();
        }

        completeActiveConsumers(eventId);
    }

    @Override
    public void complete(String eventId) {
        requireNotBlank("eventId", eventId);

        Instance instance = eventMap.get(eventId);
        if (instance == null) {
            throw new IllegalStateException("EventId not found: " + eventId);
        }

        instance.complete();
    }

    @Override
    public void delete(String eventId) {
        requireNotBlank("eventId", eventId);

        Instance instance = eventMap.remove(eventId);
        if (instance != null) {
            instance.forceComplete();
        }

        completeActiveConsumers(eventId);
    }

    @Override
    public void exists(String eventId, Consumer<Boolean> callback) {
        requireNotBlank("eventId", eventId);
        Objects.requireNonNull(callback, "callback can not be null");
        callback.accept(eventMap.containsKey(eventId));
    }

    /**
     * 程序关闭时调用，取消定时器并释放本地数据。
     */
    public void shutdown() {
        vertx.cancelTimer(cleanerTimerId);

        activeConsumerMap.forEach((key, session) -> session.complete());
        activeConsumerMap.clear();

        eventMap.forEach((eventId, instance) -> instance.forceComplete());
        eventMap.clear();
    }

    // -------------------------------------------------------------------------

    private void localLoop(String eventId,
                           String consumerId,
                           ActiveConsumer session,
                           long afterIndex,
                           Consumer<String> onNext) {
        if (!isConsumerActive(session)) {
            return;
        }

        Instance instance = eventMap.get(eventId);
        if (instance == null) {
            session.complete();
            return;
        }

        long currentIndex = afterIndex;

        for (Map.Entry<Long, String> entry : instance.tail(afterIndex).entrySet()) {
            if (!isConsumerActive(session)) {
                return;
            }

            Long messageIndex = entry.getKey();
            String message = entry.getValue();

            if (messageIndex == null || messageIndex <= currentIndex) {
                continue;
            }

            try {
                onNext.accept(message);
            } catch (Exception e) {
                System.err.println("mem onNext failed, eventId=" + eventId
                        + ", consumerId=" + consumerId
                        + ", index=" + messageIndex
                        + ", err=" + e.getMessage());

                long retryIndex = currentIndex;
                vertx.setTimer(RETRY_INTERVAL_MS, ignored ->
                        localLoop(eventId, consumerId, session, retryIndex, onNext));
                return;
            }

            currentIndex = messageIndex;
        }

        if (instance.isCompleted()) {
            session.complete();
            return;
        }

        long nextIndex = currentIndex;
        vertx.setTimer(POLL_INTERVAL_MS, ignored ->
                localLoop(eventId, consumerId, session, nextIndex, onNext));
    }

    private void cleanExpiredEvents() {
        long now = System.currentTimeMillis();

        eventMap.forEach((eventId, instance) -> {
            if (instance.isExpired(now)) {
                boolean removed = eventMap.remove(eventId, instance);
                if (removed) {
                    instance.forceComplete();
                    completeActiveConsumers(eventId);
                }
            }
        });
    }

    private void completeActiveConsumers(String eventId) {
        String prefix = eventId + "::";

        activeConsumerMap.forEach((key, session) -> {
            if (key.startsWith(prefix)) {
                session.complete();
            }
        });
    }

    private boolean isConsumerActive(ActiveConsumer session) {
        ActiveConsumer current = activeConsumerMap.get(session.activeKey);
        return current == session && !session.completed;
    }

    private static String activeConsumerKey(String eventId, String consumerId) {
        return eventId + "::" + consumerId;
    }

    private static void requireNotBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " can not be blank");
        }
    }

    // -------------------------------------------------------------------------

    static class Instance {

        /**
         * index -> message
         * <p>
         * consumer(index=12) 时，可以直接 tailMap(12, false)，从 13 开始。
         */
        private final ConcurrentSkipListMap<Long, String> messageLog = new ConcurrentSkipListMap<>();

        private final AtomicLong maxIndex = new AtomicLong(0);

        @Getter
        private volatile boolean completed = false;

        private volatile long expireAt = System.currentTimeMillis() + ACTIVE_TTL_MS;

        public synchronized void publish(Long index, String message) {
            if (completed) {
                throw new IllegalStateException("Queue already completed");
            }

            long lastIndex = maxIndex.get();
            if (index <= lastIndex) {
                throw new IllegalArgumentException(
                        "index must be strictly increasing, lastIndex=" + lastIndex + ", currentIndex=" + index
                );
            }

            messageLog.put(index, message);
            maxIndex.set(index);

            /*
             * 每次 publish 续期到 10 分钟。
             */
            expireAt = System.currentTimeMillis() + ACTIVE_TTL_MS;
        }

        public synchronized void complete() {
            if (completed) {
                return;
            }

            completed = true;

            /*
             * complete 后保留 1 分钟。
             */
            expireAt = System.currentTimeMillis() + COMPLETE_TTL_MS;
        }

        public synchronized void forceComplete() {
            completed = true;
            expireAt = System.currentTimeMillis();
        }

        public boolean isExpired(long now) {
            return now >= expireAt;
        }

        public ConcurrentSkipListMap<Long, String> tail(long afterIndex) {
            return new ConcurrentSkipListMap<>(messageLog.tailMap(afterIndex, false));
        }
    }

    private class ActiveConsumer {

        private final String activeKey;

        private final long token;

        private final Runnable onComplete;

        private volatile boolean completed = false;

        private ActiveConsumer(String activeKey, long token, Runnable onComplete) {
            this.activeKey = activeKey;
            this.token = token;
            this.onComplete = onComplete;
        }

        private void complete() {
            if (completed) {
                return;
            }

            completed = true;
            activeConsumerMap.remove(activeKey, this);

            try {
                onComplete.run();
            } catch (Exception e) {
                System.err.println("onComplete failed, activeKey=" + activeKey
                        + ", token=" + token
                        + ", err=" + e.getMessage());
            }
        }
    }
}