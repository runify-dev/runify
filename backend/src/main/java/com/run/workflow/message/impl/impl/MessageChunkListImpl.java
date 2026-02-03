package com.run.workflow.message.impl.impl;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.message.struct.AnswerContent;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.Message;
import com.run.workflow.message.struct.chunk.MessageChunk;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MessageChunkListImpl {
    private final static MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final static List<Class<?>> contentImpl = List.of(TextContentChunkListImpl.class, ReasoningChunkListImpl.class, JsonChunkListImpl.class);
    private final static List<DefaultKeyValue<MethodHandle, MethodHandle>> handle;

    static {
        handle = new ArrayList<>();
        for (Class<?> clazz : contentImpl) {
            try {
                MethodHandle support = MethodHandles.lookup().findStatic(
                        clazz, "support",
                        MethodType.methodType(Boolean.class, ContentTypeConstants.class)
                );
                MethodHandle toBlock = MethodHandles.lookup().findStatic(
                        clazz, "toBlock",
                        MethodType.methodType(List.class, List.class)
                );
                handle.add(new DefaultKeyValue<>(support, toBlock));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static List<Message> toBlock(List<MessageChunk> self) {
        HashMap<Integer, MessageConstants> messageMap = new HashMap<>();
        ConcurrentHashMap<Integer, DefaultKeyValue<ContentTypeConstants, List<Content>>> cache = new ConcurrentHashMap<>();
        List<String> index = new ArrayList<>();
        for (MessageChunk chunk : self) {
            MessageConstants messageConstants = chunk.type();
            List<AnswerContent> contents = chunk.content();
            for (AnswerContent content : contents) {
                String id = content.getRealNodeId();
                ContentTypeConstants type = content.getType();
                String v = id + "_" + type;
                int i = index.indexOf(v);
                if (i < 0) {
                    i = index.size();
                    index.add(v);
                }

                DefaultKeyValue<ContentTypeConstants, List<Content>> r = cache.computeIfAbsent(i, k -> new DefaultKeyValue<>(type, new ArrayList<>()));
                r.getValue().add(content);
                if (!messageMap.containsKey(i)) {
                    messageMap.put(i, messageConstants);
                }
            }
        }
        List<Message> result = new ArrayList<>();
        List<Content> cacheContent = new ArrayList<>();
        MessageConstants current = null;
        for (int i = 0; i < cache.size(); i++) {
            DefaultKeyValue<ContentTypeConstants, List<Content>> r = cache.get(i);
            if (current == null) {
                current = messageMap.get(i);
            } else if (current != messageMap.get(i)) {
                result.add(new Message(current, cacheContent));
                cacheContent = new ArrayList<>();
                current = messageMap.get(i);
            }
            for (DefaultKeyValue<MethodHandle, MethodHandle> methodHandleMethodHandleKeyValue : handle) {
                try {
                    Boolean support = (Boolean) methodHandleMethodHandleKeyValue.getKey().invoke(r.getKey());
                    if (support) {
                        List<Content> contents = (List<Content>) methodHandleMethodHandleKeyValue.getValue().invoke(r.getValue());
                        cacheContent.addAll(contents);
                    }
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
        if (!cacheContent.isEmpty()) {
            result.add(new Message(current, cacheContent));
        }
        return result;
    }
}
