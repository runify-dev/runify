package com.run.dao.common.convert.postgres.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For({UUID.class})
public class UUIDConverter extends AbstractConverter<UUID, UUID> {
    @SneakyThrows
    @Override
    public UUID serialize(Object from, String column) {
        return (UUID) getValue(from, column);
    }

    @Override
    public UUID deserialize(Row row, String column) {
        return row.getUUID(column);
    }
}