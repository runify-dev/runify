package com.run.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.run.handler.file.IFileHandler;
import com.run.handler.file.impl.FileHandlerImpl;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.impl.KnowledgeHandlerImpl;
import com.run.handler.node.INodeHandler;
import com.run.handler.node.impl.NodeHandlerImpl;
import com.run.handler.user.IUserHandler;
import com.run.handler.user.impl.UserHandlerImpl;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/19  18:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class HandlerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Key.get(IUserHandler.class)).to(UserHandlerImpl.class);
        bind(Key.get(INodeHandler.class)).to(NodeHandlerImpl.class);
        bind(Key.get(IKnowledgeHandler.class)).to(KnowledgeHandlerImpl.class);
        bind(Key.get(IFileHandler.class)).to(FileHandlerImpl.class);
    }
}
