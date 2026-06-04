package com.run.handler.knowledge;

import com.run.dao.entity.Knowledge;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

public interface IKnowledgeHandler extends IResourceHandler<Knowledge> {

    void edit(RoutingContext context);
}
