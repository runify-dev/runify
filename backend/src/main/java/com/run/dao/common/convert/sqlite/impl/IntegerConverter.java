package com.run.dao.common.convert.sqlite.impl;

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
@For({Integer.class, int.class})
public class IntegerConverter extends AbstractConverter<Integer, Integer> {
    @SneakyThrows
    @Override
    public Integer serialize(Object from, String column) {
        return (Integer) getValue(from, column);
    }

    @Override
    public Integer deserialize(Row row, String column) {
        return row.getInteger(column);
    }
}