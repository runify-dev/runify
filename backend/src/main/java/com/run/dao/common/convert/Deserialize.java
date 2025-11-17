package com.run.dao.common.convert;

import io.vertx.sqlclient.Row;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  18:26}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Deserialize<F> {
    F deserialize(Row row, String column, Class<?> real);
}
