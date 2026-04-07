package com.run.common.queue;

import com.run.workflow.message.struct.Content;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  22:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MemMessageQueue implements MessageQueue<Content> {
    private final static ConcurrentMap<String, Instance> eventMap = new ConcurrentHashMap<>();

    @Override
    public void consume(String eventId, String consumerId, Consumer<Content> call) {
        eventMap.get(eventId).consume(consumerId, call);
    }

    @Override
    public void publish(String eventId, Content message) {
        Instance instance = eventMap.computeIfAbsent(eventId, id -> new Instance());
        instance.publish(message);
    }

    @Override
    public void create(String eventId) {
        eventMap.put(eventId, new Instance());
    }

    @Override
    public void delete(String eventId) {
        eventMap.remove(eventId);
    }

    class Instance {
        private final List<Content> messageLog = new CopyOnWriteArrayList<>();
        private final ConcurrentMap<String, AtomicLong> consumerOffsets = new ConcurrentHashMap<>();
        private final BlockingQueue<String> newMessageSignal = new LinkedBlockingQueue<>();

        public void publish(Content message) {
            // 1. 将消息追加到日志末尾
            messageLog.add(message);
            // 2. 通知所有阻塞等待的消费者有新消息到达
            newMessageSignal.offer("NEW_MESSAGE");
        }


        public void consume(String consumerId, Consumer<Content> call) {
            // 获取或初始化该消费者的偏移量（从0开始即为从头消费）
            AtomicLong offset = consumerOffsets.computeIfAbsent(consumerId, id -> new AtomicLong(0));
            long currentOffset = offset.get();
            // 判断是否有积压的消息可以重播
            if (currentOffset < messageLog.size()) {
                // 从日志中获取该偏移量对应的消息
                Content messageChunk = messageLog.get((int) currentOffset);
                // 成功消费后，将该消费者的偏移量加1
                offset.incrementAndGet();
                call.accept(messageChunk);
            } else {
                // 如果没有积压消息，则阻塞等待新消息信号
                try {
                    newMessageSignal.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 被唤醒后，递归调用自身以再次尝试消费（此时offset已落后于log size）
                consume(consumerId, call);
            }
        }
    }
}
