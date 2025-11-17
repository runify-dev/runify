package com.run.dao.common.convert;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:10}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TypeConverter {
    /**
     * 将Object
     * @param value
     * @return
     */
    public static Integer toInteger(Object value) {
        return switch (value) {
            case Number n -> n.intValue();
            case String s -> Integer.parseInt(s);
            case Boolean b -> b ? 1 : 0;
            default -> throw new IllegalArgumentException("无法转换为 Integer: " + value);
        };
    }

    public static int toInt(Object value) {
        Integer result = toInteger(value);
        return result != null ? result : 0;
    }
}
