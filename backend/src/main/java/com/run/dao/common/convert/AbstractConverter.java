package com.run.dao.common.convert;

import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  16:09}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class AbstractConverter<F, T> implements Converter<F, T> {
    private static final Map<Class<?>, Map<String, MethodHandle>> HANDLE_CACHE = new ConcurrentHashMap<>();

    @SneakyThrows
    protected Object getValue(Object from, String fieldName) {
        MethodHandle orCreateHandle = getOrCreateHandle(from.getClass(), fieldName);
        return orCreateHandle.invoke(from);
    }

    protected MethodHandle getOrCreateHandle(Class<?> clazz, String fieldName) {
        return HANDLE_CACHE
                .computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(fieldName, k -> {
                    try {
                        Field field = FieldUtils.getField(clazz, k, true);
                        if (field == null) return null;
                        MethodHandles.Lookup lookup = MethodHandles.lookup();
                        return lookup.unreflectGetter(field);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                });
    }

    @Override
    public F deserialize(Row row, String column, Class<?> real) {
        return deserialize(row, column);
    }

    public F deserialize(Row row, String column) {
        return null;
    }
}
