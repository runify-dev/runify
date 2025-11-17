package com.run.dao.common.convert.sqlite.impl;

import com.run.common.util.JacksonUtils;
import com.run.dao.common.convert.AbstractConverter;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(JsonObject.class)
public class JsonObjectConverter extends AbstractConverter<JsonObject, String> {
    @SneakyThrows
    @Override
    public String serialize(Object from, String column) {
        JsonObject value = (JsonObject) getValue(from, column);
        return value == null ? null : JacksonUtils.toJson(value);
    }

    @Override
    public JsonObject deserialize(Row row, String column) {
        String value = row.getString(column);
        if (StringUtils.isNotEmpty(value)) {
            return JacksonUtils.fromJson(row.getString(column), JsonObject.class);
        }
        return null;
    }
}