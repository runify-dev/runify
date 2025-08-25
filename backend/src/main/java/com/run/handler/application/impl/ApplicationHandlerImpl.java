package com.run.handler.application.impl;


import com.run.common.constants.ConversationUserType;
import com.run.common.openai.request.message.UserMessage;
import com.run.common.result.Result;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Application;
import com.run.dao.entity.Conversation;
import com.run.dao.entity.ConversationRecord;
import com.run.dao.entity.User;
import com.run.dao.mapper.ApplicationMapper;
import com.run.dao.mapper.ConversationMapper;
import com.run.dao.mapper.ConversationRecordMapper;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.pojo.ChatPojo;
import com.run.handler.application.pojo.ConversationQuery;
import com.run.handler.application.pojo.EditApplicationPojo;
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
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationHandlerImpl implements IApplicationHandler {

    protected ApplicationMapper applicationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationRecordMapper conversationRecordMapper;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper, ConversationMapper conversationMapper, ConversationRecordMapper conversationRecordMapper) {
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
    public void get(RoutingContext context) {
        String node_id = context.pathParam("node_id");
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

    public Expression getConversationQuery(ConversationQuery query) {
        List<Expression> expressions = new ArrayList<>();
        Expression applicationIdEq = new EqualsTo().withLeftExpression(new Column("application_id"))
                .withRightExpression(new StringValue(query.getApplicationId()));
        String startTime = query.getStartTime();
        if (StringUtils.isNotEmpty(startTime)) {
            MinorThan createTime = new MinorThan()
                    .withLeftExpression(new Column("create_time"))
                    .withRightExpression(new StringValue(startTime));
            expressions.add(createTime);
        }
        if (StringUtils.isNotEmpty(query.getEndTime())) {
            GreaterThan createTime = new GreaterThan().withLeftExpression(new Column("create_time"))
                    .withRightExpression(new StringValue(query.getEndTime()));
            expressions.add(createTime);
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            LikeExpression name = new LikeExpression()
                    .withLeftExpression(new Column("name"))
                    .withRightExpression(new StringValue(query.getName()));
            expressions.add(name);
        }
        return expressions.stream().reduce(applicationIdEq, (x, y) ->
                new AndExpression().withLeftExpression(x).withRightExpression(y));
    }

    @Override
    public void pageConversation(RoutingContext context) {
        String currentPage = context.pathParam("currentPage");
        String pageSize = context.pathParam("pageSize");
        MultiMap entries = context.queryParams().copy();
        entries.addAll(context.pathParams());
        Expression conversationQuery = getConversationQuery(new ConversationQuery(entries));
        conversationMapper.page(conversationQuery, Long.parseLong(currentPage), Long.parseLong(pageSize))
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageConversationRecord(RoutingContext context) {

    }
}
