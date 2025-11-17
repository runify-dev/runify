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
@For(Enum.class)
public class EnumConverter extends AbstractConverter<Enum<?>, String> {
    @SneakyThrows
    @Override
    public String serialize(Object from, String column) {
        Enum<?> enumValue = (Enum<?>) getValue(from, column);
        if (enumValue == null) {
            return null;
        }
        return enumValue.name();
    }

    private Enum<?> deserializeFromString(String value, Class<?> real) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (real != null && real.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<Enum> enumClass = (Class<Enum>) real;
            return Enum.valueOf(enumClass, value.trim());
        }
        return null;
    }

    @Override
    public Enum<?> deserialize(Row row, String column, Class<?> real) {
        return deserializeFromString(row.getString(column), real);
    }
}