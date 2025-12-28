package com.run.common.query.impl;

import com.run.common.query.Deserialize;
import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For(LocalDateTime.class)
public class LocalDateTimeConverter implements Deserialize<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(RoutingContext context, QueryParams queryParams, Class<?> real) {
        LocationConstants location = queryParams.location();
        String value;
        if (location == LocationConstants.PATH) {
            value = context.pathParam(queryParams.name());
        } else {
            MultiMap entries = context.queryParams();
            value = entries.get(queryParams.name());
        }
        return StringUtils.isEmpty(value) ? null : LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}