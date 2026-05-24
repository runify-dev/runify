package com.run.dao.common.convert.sqlite.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.sqlclient.Row;

import java.time.LocalDate;

@For(LocalDate.class)
public class LocalDateConverter extends AbstractConverter<LocalDate, LocalDate> {
    @Override
    public LocalDate deserialize(Row row, String column) {
        String value = row.getString(column);
        return value == null ? null : LocalDate.parse(value);
    }

    @Override
    public LocalDate serialize(Object from, String column) {
        return (LocalDate) getValue(from, column);
    }
}
