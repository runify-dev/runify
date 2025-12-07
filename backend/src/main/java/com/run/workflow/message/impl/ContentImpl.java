package com.run.workflow.message.impl;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.message.struct.Content;
import io.vertx.core.json.JsonObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  18:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ContentImpl {
    private final static MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final static List<Class<?>> contentImpl = List.of(
            TextContentImpl.class,
            QuestionContentImpl.class,
            ReasoningContentImpl.class);
    private final static List<DefaultKeyValue<MethodHandle, MethodHandle>> newInstance;

    static {
        newInstance = new ArrayList<>();
        for (Class<?> clazz : contentImpl) {
            try {
                MethodHandle support = lookup.findStatic(
                        clazz, "support",
                        MethodType.methodType(Boolean.class, ContentTypeConstants.class)
                );
                MethodHandle toChunk = lookup.findStatic(
                        clazz, "newInstance",
                        MethodType.methodType(Content.class, JsonObject.class)
                );
                newInstance.add(new DefaultKeyValue<>(support, toChunk));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Content newInstance(JsonObject jsonObject) {
        for (DefaultKeyValue<MethodHandle, MethodHandle> kv : newInstance) {
            ContentTypeConstants type = ContentTypeConstants.valueOf(jsonObject.getString("type"));
            try {
                Boolean ok = (Boolean) kv.getKey().invoke(type);
                if (ok) {
                    return (Content) kv.getValue().invoke(jsonObject);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        throw new NullPointerException("");
    }


}
