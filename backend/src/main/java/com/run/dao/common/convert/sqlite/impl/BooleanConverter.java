package com.run.dao.common.convert.sqlite.impl;

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
public class BooleanConverter extends AbstractConverter<Boolean, Integer> {
    @SneakyThrows
    @Override
    public Integer serialize(Object from, String column) {
        Boolean value = (Boolean) getValue(from, column);
        return value == null ? null : value ? 1 : 0;
    }

    @Override
    public Boolean deserialize(Row row, String column) {
        return row.getInteger(column) != 0;
    }
}