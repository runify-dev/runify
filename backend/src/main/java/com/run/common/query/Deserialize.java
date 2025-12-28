package com.run.common.query;

import com.run.common.query.annotations.QueryParams;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  21:37}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Deserialize<T> {
    T deserialize(RoutingContext context, QueryParams queryParams, Class<?> real);
}
