package com.run.common.query.impl;

import com.run.common.query.Deserialize;
import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(List.class)
public class ListDeserialize implements Deserialize<List<?>> {


    @Override
    public List<?> deserialize(RoutingContext context, QueryParams queryParams, Class<?> real) {
        LocationConstants location = queryParams.location();
        List<String> value;
        if (location == LocationConstants.PATH) {
            value = List.of(context.pathParam(queryParams.name()));
        } else {
            MultiMap entries = context.queryParams();
            value = entries.getAll(queryParams.name());
        }
        if (value == null) {
            return null;
        }
        if (real.isAssignableFrom(String.class)) {
            return value;
        } else if (real.isAssignableFrom(Double.class)) {
            return value.stream().map(Double::parseDouble).toList();
        } else if (real.isAssignableFrom(Float.class)) {
            return value.stream().map(Float::parseFloat).toList();
        } else if (real.isAssignableFrom(Integer.class)) {
            return value.stream().map(Integer::parseInt).toList();
        } else if (real.isAssignableFrom(UUID.class)) {
            return value.stream().map(UUID::fromString).toList();
        } else if (real.isAssignableFrom(Long.class)) {
            return value.stream().map(Long::parseLong).toList();
        }
        throw new RuntimeException("不支持的子类型");
    }

}