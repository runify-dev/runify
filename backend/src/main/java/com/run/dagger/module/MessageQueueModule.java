package com.run.dagger.module;

import com.run.common.queue.MemMessageQueue;
import com.run.common.queue.MessageQueue;

import com.run.workflow.message.struct.Content;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  23:12}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Module
public class MessageQueueModule {
    @Provides
    @Singleton
    public MessageQueue<Content> messageQueue() {
        //todo 目前先用内存 以后可配置redis 用于集群
        return new MemMessageQueue();
    }
}
