package com.run.dao.common.convert;

import io.vertx.sqlclient.Row;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/21  21:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface EntityConvert<T> {
    T mapTo(Row row);

    Map<String, Object> toMap(Object from);
}
