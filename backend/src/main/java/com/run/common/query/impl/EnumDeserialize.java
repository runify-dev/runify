package com.run.common.query.impl;

import com.run.common.query.Deserialize;
import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(Enum.class)
public class EnumDeserialize implements Deserialize<Enum<?>> {
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
    public Enum<?> deserialize(RoutingContext context, QueryParams queryParams, Class<?> real) {
        LocationConstants location = queryParams.location();
        String value;
        if (location == LocationConstants.PATH) {
            value = context.pathParam(queryParams.name());
        } else {
            MultiMap entries = context.queryParams();
            value = entries.get(queryParams.name());
        }
        return StringUtils.isEmpty(value) ? null : deserializeFromString(value, real);
    }
}