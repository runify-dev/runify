package com.run.dao.common.convert.postgres.impl;

import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(JsonObject.class)
public class JsonObjectConverter extends AbstractConverter<JsonObject, JsonObject> {
    @SneakyThrows
    @Override
    public JsonObject serialize(Object from, String column) {
        return (JsonObject) getValue(from, column);
    }

    @Override
    public JsonObject deserialize(Row row, String column) {
        return row.getJsonObject(column);
    }
}