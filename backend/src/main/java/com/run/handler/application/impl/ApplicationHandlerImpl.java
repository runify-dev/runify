package com.run.handler.application.impl;

import com.google.inject.Inject;
import com.run.dao.mapper.ApplicationMapper;
import com.run.handler.application.IApplicationHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationHandlerImpl implements IApplicationHandler {
    @Inject
    protected ApplicationMapper applicationMapper;


    @Override
    public void edit(RoutingContext context) {

    }

    @Override
    public void get(RoutingContext context) {
        String node_id = context.pathParam("node_id");
    }
}
