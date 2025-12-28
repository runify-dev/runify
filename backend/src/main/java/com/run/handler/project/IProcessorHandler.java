package com.run.handler.project;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/21  18:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IProcessorHandler {
    void create(RoutingContext context);

    void page(RoutingContext context);

    void get(RoutingContext context);

    void edit(RoutingContext context);

    void deploy(RoutingContext context);

}
