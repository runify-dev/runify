package com.run.dao.common.convert;

import com.run.dao.common.annotations.Column;
import com.run.dao.entity.User;
import io.vertx.sqlclient.Row;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/21  21:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseConvert<T> {
    T mapTo(Row row);

    default Map<String, Object> toMap(T t) {
        Class<?> aClass = t.getClass();
        Field[] fields = aClass.getDeclaredFields();
        Map<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                try {
                    field.setAccessible(true);
                    Column annotation = field.getAnnotation(Column.class);
                    result.put(annotation.name(), field.get(t));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

}
