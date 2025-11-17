package com.run.dao.common.convert.sqlite.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

import java.time.LocalDateTime;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(LocalDateTime.class)
public class LocalDateTimeConverter extends AbstractConverter<LocalDateTime, LocalDateTime> {
    @SneakyThrows
    @Override
    public LocalDateTime serialize(Object from, String column) {
        return (LocalDateTime) getValue(from, column);
    }

    @Override
    public LocalDateTime deserialize(Row row, String column) {
        return row.getLocalDateTime(column);
    }
}