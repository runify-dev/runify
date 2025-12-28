package com.run.common.query.impl;

import com.run.common.query.Deserialize;
import com.run.common.query.annotations.QueryParams;
import com.run.common.query.constants.LocationConstants;
import com.run.dao.common.convert.annotations.For;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/15  15:34}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@For({String.class})
public class StringConverter implements Deserialize<String> {


    @Override
    public String deserialize(RoutingContext context, QueryParams queryParams, Class<?> real) {
        LocationConstants location = queryParams.location();
        String value;
        if (location == LocationConstants.PATH) {
            value = context.pathParam(queryParams.name());
        } else {
            MultiMap entries = context.queryParams();
            value = entries.get(queryParams.name());
        }
        return value;
    }
}