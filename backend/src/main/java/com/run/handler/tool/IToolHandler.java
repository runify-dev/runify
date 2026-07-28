package com.run.handler.tool;

import com.run.dao.entity.Tool;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

public interface IToolHandler extends IResourceHandler<Tool> {

    void edit(RoutingContext context);
}
