package com.run.handler.application.impl;


import com.run.common.result.Result;
import com.run.dao.entity.Application;
import com.run.dao.entity.Knowledge;
import com.run.dao.mapper.ApplicationMapper;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.pojo.EditApplicationPojo;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationHandlerImpl implements IApplicationHandler {

    protected ApplicationMapper applicationMapper;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditApplicationPojo pojo = context.body().asPojo(EditApplicationPojo.class);
        JsonObject workflow = pojo.getWorkflow();
        Application application = new Application();
        application.setId(UUID.fromString(resourceId));
        application.setWorkflow(workflow);
        applicationMapper.update(application).onSuccess(ok -> {
            context.end(Result.success(true).toBuffer());
        }).onFailure(context::fail);

    }

    @Override
    public void get(RoutingContext context) {
        String node_id = context.pathParam("node_id");
    }
}
