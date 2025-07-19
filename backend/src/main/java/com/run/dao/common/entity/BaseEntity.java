package com.run.dao.common.entity;

import com.run.dao.common.annotations.Column;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.TupleMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseEntity<T> {
    default Map<String, Object> toMap() {
        Class<?> aClass = this.getClass();
        Field[] fields = aClass.getDeclaredFields();
        Map<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                try {
                    field.setAccessible(true);
                    Column annotation = field.getAnnotation(Column.class);
                    result.put(annotation.name(), field.get(this));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    T mapTo(Row row);
}
