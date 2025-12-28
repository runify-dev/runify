package com.run.common.pojo;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  22:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public record FieldMapping(Field field, String columnName, MethodHandle getter, MethodHandle setter) {
    public void setValue(Object instance, Object value) throws Throwable {
        setter.invoke(instance, value);
    }

    public Object getValue(Object instance) throws Throwable {
        return getter.invoke(instance);
    }
}