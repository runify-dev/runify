package com.run.dao.common.convert.postgres.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For({Long.class, long.class})
public class LongConverter extends AbstractConverter<Long, Long> {
    @SneakyThrows
    @Override
    public Long serialize(Object from, String column) {
        return (Long) getValue(from, column);
    }

    @Override
    public Long deserialize(Row row, String column) {
        return row.getLong(column);
    }
}