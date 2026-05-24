package com.run.dao.common.convert.postgres.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(Boolean.class)
public class BooleanConverter extends AbstractConverter<Boolean, Boolean> {
    @SneakyThrows
    @Override
    public Boolean serialize(Object from, String column) {
        return (Boolean) getValue(from, column);
    }

    @Override
    public Boolean deserialize(Row row, String column) {
        return row. getBoolean(column);
    }
}