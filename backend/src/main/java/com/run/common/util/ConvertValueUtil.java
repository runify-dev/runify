package com.run.common.util;

import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  23:01}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConvertValueUtil {

    /**
     * 转换对象
     *
     * @param value 对象
     * @return 对象
     */
    public static Object convertValue(Value value) {
        if (value == null || value.isNull() || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();

        if (value.isNumber()) {
            if (value.fitsInInt()) return value.asInt();
            if (value.fitsInLong()) return value.asLong();
            if (value.fitsInDouble()) return value.asDouble();
            if (value.fitsInFloat()) return value.asFloat();
            return value.asLong();
        }

        if (value.hasArrayElements()) {
            return convertArray(value);
        }

        if (value.hasMembers()) {
            return convertObject(value);
        }

        // 保留原始 Value 用于其他操作
        return value;
    }

    private static List<Object> convertArray(Value array) {
        long size = array.getArraySize();
        List<Object> result = new ArrayList<>((int) size);

        for (long i = 0; i < size; i++) {
            result.add(convertValue(array.getArrayElement(i)));
        }

        return result;
    }

    private static Map<String, Object> convertObject(Value obj) {
        Map<String, Object> result = new HashMap<>();

        for (String key : obj.getMemberKeys()) {
            Value member = obj.getMember(key);
            result.put(key, convertValue(member));
        }

        return result;
    }

    private static <T> T mapToObject(Map<String, Object> map, Class<T> targetClass) {
        try {
            T instance = targetClass.getDeclaredConstructor().newInstance();

            for (java.lang.reflect.Field field : targetClass.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = map.get(field.getName());

                if (value != null && field.getType().isAssignableFrom(value.getClass())) {
                    field.set(instance, value);
                } else if (value instanceof Map && !field.getType().isPrimitive()) {
                    // 嵌套对象转换
                    Object nested = mapToObject((Map<String, Object>) value, field.getType());
                    field.set(instance, nested);
                } else if (value instanceof List) {
                    // 列表类型处理
                    field.set(instance, value);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("映射对象失败", e);
        }
    }
}