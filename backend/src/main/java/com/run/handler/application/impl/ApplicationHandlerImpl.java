package com.run.handler.application.impl;


import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.ConversationUserConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.F;
import com.run.dao.entity.*;
import com.run.dao.mapper.*;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.dto.ConversationDTO;
import com.run.handler.application.pojo.ConversationQuery;
import com.run.handler.application.pojo.EditApplicationPojo;
import com.run.handler.application.vo.ConversationVO;
import com.run.handler.application.vo.CreateConversationVO;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.Message;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationHandlerImpl extends ResourceHandlerImpl<Application, ApplicationFolder, ApplicationPermission, ApplicationRelation, ApplicationMapper, ApplicationFolderMapper, ApplicationPermissionMapper, ApplicationRelationMapper> implements IApplicationHandler {

    protected ApplicationMapper applicationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper,
                                  ApplicationFolderMapper applicationFolderMapper,
                                  ApplicationRelationMapper applicationRelationMapper,
                                  ApplicationPermissionMapper applicationPermissionMapper,
                                  ConversationMapper conversationMapper,
                                  ConversationMessageMapper conversationMessageMapper) {
        super(applicationMapper, applicationFolderMapper, applicationRelationMapper, applicationPermissionMapper);
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
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
    protected SimpleNodePojo resourceToSimpleNodePojo(Application application) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(application, simpleNodePojo);
        simpleNodePojo.setType("application");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(ApplicationFolder applicationFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(applicationFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    protected ApplicationRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ApplicationRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Application resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Application resource, String name) {
        resource.setName(name);
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
    protected String getName(Application resource) {
        return "";
    }

    @Override
    protected UUID getTarget(ApplicationPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(ApplicationPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建应用";
    }

    @Override
    protected Application newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Application(resourceId, parentUuId, name, "", "", new JsonObject(), new JsonObject(), false, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected ApplicationPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new ApplicationPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }


    @Override
    public void createConversation(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        User user = context.user().get("user");
        CreateConversationVO conversationVO = context.body().asPojo(CreateConversationVO.class);
        Conversation conversation = new Conversation(UUID.randomUUID(),
                UUID.fromString(applicationId),
                conversationVO.name(),
                ConversationExecuteConstants.DEBUG, new JsonObject(),
                user.getId().toString(),
                ConversationUserConstants.ADMIN_USER,
                0, 0, 0, 0,
                Boolean.FALSE, LocalDateTime.now(), LocalDateTime.now());
        conversationMapper.save(conversation).onSuccess(ok -> {
            ConversationDTO result = new ConversationDTO(conversation.getId(),
                    conversation.getApplicationId(),
                    conversation.getName(),
                    conversation.getExecuteType(),
                    conversation.getCreateTime(),
                    conversation.getUpdateTime());
            context.end(Result.success(result).toBuffer());
        }).onFailure(context::fail);

    }

    @Override
    public void chat(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String conversationId = context.pathParam("conversationId");
        ConversationVO conversationVO = context.body().asPojo(ConversationVO.class);
        com.run.workflow.message.struct.QuestionContent questionContent = new com.run.workflow.message.struct.QuestionContent(conversationVO.getContent().getContent(), conversationVO.getWorkflowRunId());
        ConversationMessage conversationMessage = new ConversationMessage(UUID.randomUUID(),
                UUID.fromString(conversationId),
                UUID.fromString(applicationId), UUID.fromString(conversationVO.getWorkflowRunId()),
                MessageConstants.USER,
                new JsonArray(List.of(questionContent)),
                LocalDateTime.now(),
                LocalDateTime.now());

        Future<Application> applicationFuture = applicationMapper.getById(applicationId);

        Future<List<ConversationMessage>> conversationMessageFuture = conversationMessageMapper.save(conversationMessage)
                .compose(ok -> conversationMessageMapper
                        .list(conversationMessageMapper.select().where(F.field(ConversationMessage::getConversationId)
                                                .eq(F.params(ConversationMessage::getConversationId)))
                                        .orderBy(F.field(ConversationMessage::getCreateTime).desc())
                                        .limit(DSL.param("#{limit}", Integer.class)).getSQL(ParamType.NAMED),
                                Map.of("conversationId", conversationId, "limit", 10)));
        Future.all(applicationFuture, conversationMessageFuture)
                .onSuccess(ok -> extracted(context, ((Application) ok.resultAt(0)).getWorkflow(),
                        UUID.fromString(conversationId), UUID.fromString(applicationId),
                        UUID.fromString(conversationVO.getWorkflowRunId()), ok.resultAt(1)))
                .onFailure(context::fail);
    }


    private void extracted(RoutingContext context,
                           JsonObject workflow,
                           UUID conversationId,
                           UUID applicationId,
                           UUID workflowRunId,
                           List<ConversationMessage> conversationMessages) {
        List<ConversationMessage> list = conversationMessages.stream().sorted(Comparator.comparing(ConversationMessage::getCreateTime)).toList();
        context.response().setChunked(true);
        context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
        context.response().putHeader("Cache-Control", "no-cache");
        context.response().putHeader("Character-Encoding", "utf-8");
        context.response().write(Buffer.buffer("", "utf-8"));
        WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow, WorkflowType.CHAT_WORKFLOW),
                new HashMap<>(Map.of(
                        "messages", list,
                        "conversationId", conversationId,
                        "applicationId", applicationId)),
                new HashMap<>(), (wm, node, chunk, isEnd) -> {
            if (isEnd) {
                List<Content> chunks = wm.getChunks();
                List<ConversationMessage> messageArrayList = new ArrayList<>();
                ConversationMessage conversationMessage = new ConversationMessage(UUID.randomUUID(),
                        conversationId,
                        applicationId, workflowRunId,
                        MessageConstants.ASSISTANT,
                        new JsonArray(chunks),
                        LocalDateTime.now(),
                        LocalDateTime.now());
                messageArrayList.add(conversationMessage);
                conversationMessageMapper.batch_save(messageArrayList).onSuccess(_ -> {
                            context.response().end();
                        })
                        .onFailure(e -> {
                            context.fail(e);
                        });
                return;
            }

            context.response().write(Buffer.buffer("data: " + JacksonUtils.toJson(chunk) + "\n\n", "utf-8"));
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
            condition = condition.and(DSL.field("name").like(F.params(Conversation::getName)));
        }
        if (StringUtils.isNotEmpty(query.getExecuteType())) {
            condition = condition.and(DSL.field("execute_type").eq(DSL.param("#{execute_type}")));
        }
        return condition;
    }

    @Override
    public void pageConversation(RoutingContext context) {
        String currentPage = context.queryParams().get("currentPage");
        String pageSize = context.queryParams().get("pageSize");
        MultiMap entries = context.queryParams().copy();
        entries.addAll(context.pathParams());
        ConversationQuery conversation = new ConversationQuery(entries);
        Condition conversationQuery = getConversationQuery(conversation);
        conversationMapper.page(conversationQuery,
                        List.of(F.field(Conversation::getUpdateTime).desc()),
                        Long.parseLong(currentPage), Long.parseLong(pageSize),
                        Map.of("application_id", conversation.getApplicationId(),
                                "name", StringUtils.isEmpty(conversation.getName()) ? "" : "%" + conversation.getName() + "%",
                                "execute_type", StringUtils.isEmpty(conversation.getExecuteType()) ? "" : conversation.getExecuteType()))
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageConversationMessage(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String conversationId = context.pathParam("conversationId");
        String currentPage = context.queryParams().get("currentPage");
        String pageSize = context.queryParams().get("pageSize");
        conversationMessageMapper.page(F.field(ConversationMessage::getApplicationId).eq(F.params(ConversationMessage::getApplicationId))
                                .and(F.field(ConversationMessage::getConversationId).eq(F.params(ConversationMessage::getConversationId))),
                        List.of(F.field(Conversation::getUpdateTime).desc()),
                        Long.parseLong(currentPage),
                        Long.parseLong(pageSize),
                        Map.of("conversationId", conversationId, "applicationId", applicationId)
                ).onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);

    }
}
