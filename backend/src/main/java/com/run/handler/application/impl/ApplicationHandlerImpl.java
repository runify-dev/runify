package com.run.handler.application.impl;


import com.run.common.constants.ConversationUserType;
import com.run.common.openai.request.message.UserMessage;
import com.run.common.result.Result;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.*;
import com.run.dao.mapper.ApplicationMapper;
import com.run.dao.mapper.ApplicationRelationMapper;
import com.run.dao.mapper.ConversationMapper;
import com.run.dao.mapper.ConversationRecordMapper;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.pojo.ChatPojo;
import com.run.handler.application.pojo.ConversationQuery;
import com.run.handler.application.pojo.EditApplicationPojo;
import com.run.handler.common.impl.TreeHandler;
import com.run.workflow.Answer;
import com.run.workflow.INode;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.entity.WorkFlow;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationHandlerImpl extends TreeHandler<Application, ApplicationRelation, ApplicationMapper, ApplicationRelationMapper> implements IApplicationHandler {

    protected ApplicationMapper applicationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationRecordMapper conversationRecordMapper;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper,
                                  ApplicationRelationMapper applicationRelationMapper,
                                  ConversationMapper conversationMapper,
                                  ConversationRecordMapper conversationRecordMapper) {
        super(applicationMapper, applicationRelationMapper);
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationRecordMapper = conversationRecordMapper;
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
    protected String getNodeName(Application application) {
        return application.getName();
    }

    @Override
    protected UUID getNodeId(Application application) {
        return application.getId();
    }

    @Override
    protected ApplicationRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ApplicationRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected Application newNode(UUID id, UUID parentId, String type, String name) {
        return new Application(id, parentId, name, type, "", new JsonObject(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected UUID getAncestorId(ApplicationRelation applicationRelation) {
        return applicationRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ApplicationRelation applicationRelation) {
        return applicationRelation.getDepth();
    }

    @Override
    protected Map<String, String> getNamePrefixMap() {
        return Map.of("application", "新建应用", "folder", "新建文件夹");
    }

    @Override
    public void chat(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        ChatPojo pojo = context.body().asPojo(ChatPojo.class);
        Future<Conversation> conversationFuture = conversationMapper
                .getById(pojo.getConversationId().toString())
                .compose(conversation -> {
                    if (conversation == null) {
                        Conversation conversationNew = new Conversation(UUID.randomUUID(),
                                UUID.fromString(applicationId),
                                StringUtils.substring(pojo.getQuestion().getQuestion(), 0, 128),
                                new JsonObject(), ((User) context.user().get("user")).getId(),
                                ConversationUserType.ANONYMOUS_USER,
                                0, 0, 0, 0,
                                false, LocalDateTime.now(), LocalDateTime.now());
                        return conversationMapper.save(conversationNew)
                                .compose(_ -> Future.succeededFuture(conversationNew));
                    }
                    return Future.succeededFuture(conversation);
                });
        Future<Application> applicationFuture = applicationMapper.getById(applicationId);
        Future.all(conversationFuture, applicationFuture)
                .onSuccess(ok -> extracted(context, ok, pojo))
                .onFailure(context::fail);

    }


    private void extracted(RoutingContext context, CompositeFuture ok, ChatPojo pojo) {
        Conversation conversation = ok.resultAt(0);
        Application application = ok.resultAt(1);
        JsonObject workflow = application.getWorkflow();
        context.response().setChunked(true);
        context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
        context.response().putHeader("Cache-Control", "no-cache");
        context.response().putHeader("Character-Encoding", "utf-8");
        context.response().write(Buffer.buffer("", "utf-8"));
        String conversationRecordId = UUID.randomUUID().toString();
        WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow),
                List.of(new UserMessage(pojo.getQuestion().getQuestion())),
                new HashMap<>(Map.of("conversationId", conversation.getId(),
                        "applicationId", application.getId(),
                        "conversationRecordId", conversationRecordId)),
                new HashMap<>(), (wm, node, chunk, isEnd) -> {
            if (isEnd) {
                String conversationId = wm.getParams().get("conversationId").toString();
                String applicationId = wm.getParams().get("applicationId").toString();
                List<INode<?, ?>> nodes = wm.getNodes();
                List<Answer> answers = nodes.stream().map(n -> n.getAnswerList(wm)).flatMap(Collection::stream).toList();
                ConversationRecord conversationRecord = new ConversationRecord(UUID.fromString(conversationRecordId),
                        UUID.fromString(conversationId),
                        UUID.fromString(applicationId), false, false,
                        new JsonObject(pojo.getQuestion().toMap()),
                        new JsonArray(answers),
                        new JsonObject(), wm.getRuntime(), LocalDateTime.now(), LocalDateTime.now());
                conversationRecordMapper.save(conversationRecord)
                        .onSuccess(_ -> {
                            context.response().end();
                        })
                        .onFailure(context::fail);
                return;
            }
            List<HashMap<String, Object>> list = chunk.toAppMap().stream().map(m -> {
                HashMap<String, Object> result = new HashMap<>(m);
                result.put("status", node.getStatus());
                result.put("real_node_id", node.getReal_node_id());
                result.put("node_id", node.getNode().getId());
                result.put("display_id", node.getDisplayId());
                result.put("node_name", node.getNode().getProperties().getString("name"));
                result.put("conversation_record_id", conversationRecordId);
                result.put("conversation_id", conversation.getId());
                return result;
            }).toList();
            for (HashMap<String, Object> result : list) {
                context.response().write(Buffer.buffer("data: " + JacksonUtils.toJson(result) + "\n\n", "utf-8"));
            }

        });
        workFlowManage.invoke();
    }

    public Condition getConversationQuery(ConversationQuery query) {
        Condition condition = DSL.field("application_id").eq(DSL.param("#{application_id}"));
        String startTime = query.getStartTime();
        if (StringUtils.isNotEmpty(startTime)) {
            condition = condition.and(DSL.field("create_time").le(startTime));
        }
        if (StringUtils.isNotEmpty(query.getEndTime())) {
            condition = condition.and(DSL.field("create_time").ge(startTime));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            condition = condition.and(DSL.field("name").like(query.getName()));
        }
        return condition;
    }

    @Override
    public void pageConversation(RoutingContext context) {
        String currentPage = context.pathParam("currentPage");
        String pageSize = context.pathParam("pageSize");
        MultiMap entries = context.queryParams().copy();
        entries.addAll(context.pathParams());
        ConversationQuery conversation = new ConversationQuery(entries);
        Condition conversationQuery = getConversationQuery(conversation);
        conversationMapper.page(conversationQuery, Long.parseLong(currentPage), Long.parseLong(pageSize), Map.of("application_id", conversation.getApplicationId()))
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageConversationRecord(RoutingContext context) {

    }
}
