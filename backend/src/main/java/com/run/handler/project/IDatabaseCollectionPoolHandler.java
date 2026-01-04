package com.run.handler.project;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:30}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IDatabaseCollectionPoolHandler {
    void create(RoutingContext context);
    void page(RoutingContext context);
}
