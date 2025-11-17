package com.run.dao.common.convert;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  18:25}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Serialize<T> {
    T serialize(Object from, String column);
}
