package com.run.dao.common.convert.postgres.impl;

import com.run.common.util.JacksonUtils;
import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(JsonArray.class)
public class JsonArrayConverter extends AbstractConverter<JsonArray, JsonArray> {
    @SneakyThrows
    @Override
    public JsonArray serialize(Object from, String column) {
        return (JsonArray) getValue(from, column);
    }

    @Override
    public JsonArray deserialize(Row row, String column) {
        return row.getJsonArray(column);
    }
}