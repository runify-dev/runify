package com.run.common.common_handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/5/6  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ResultHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext context) {
        context.response().putHeader("content-type", "application/json;charset=utf-8");
        context.next();
    }
}
