package com.run.handler.application.impl;


import com.run.common.openai.request.message.UserMessage;
import com.run.common.openai.response.chunk.ChoiceDelta;
import com.run.common.result.Result;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Application;
import com.run.dao.entity.Knowledge;
import com.run.dao.mapper.ApplicationMapper;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.pojo.ChatPojo;
import com.run.handler.application.pojo.EditApplicationPojo;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public void chat(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        applicationMapper.getById(resourceId).onSuccess(ok -> {
            ChatPojo pojo = context.body().asPojo(ChatPojo.class);
            JsonObject workflow = ok.getWorkflow();
            context.response().setChunked(true);
            context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
            context.response().putHeader("Cache-Control", "no-cache");
            context.response().putHeader("Character-Encoding", "utf-8");
            context.response().write(Buffer.buffer("", "utf-8"));
            WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow),
                    List.of(new UserMessage(pojo.getQuestion())),
                    new HashMap<>(),
                    new HashMap<>(), (node, chunk, isEnd) -> {
                if (isEnd) {
                    context.response().end();
                    return;
                }
                List<HashMap<String, Object>> list = chunk.toAppMap().stream().map(m -> {
                    HashMap<String, Object> result = new HashMap<>(m);
                    result.put("status", node.getStatus());
                    result.put("real_node_id", node.getReal_node_id());
                    result.put("node_id", node.getNode().getId());
                    result.put("node_name", node.getNode().getProperties().getString("name"));
                    return result;
                }).toList();
                for (HashMap<String, Object> result : list) {
                    context.response().write(Buffer.buffer("data: " + JacksonUtils.toJson(result) + "\n\n", "utf-8"));
                }

            });
            workFlowManage.invoke();
        });

    }
}
