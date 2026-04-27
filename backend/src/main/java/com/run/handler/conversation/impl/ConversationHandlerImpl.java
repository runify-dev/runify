package com.run.handler.conversation.impl;

import com.run.auth.constants.ConversationTypeConstants;
import com.run.auth.dto.ConversationTokenDTO;
import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.ConversationUserConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.query.Query;
import com.run.common.queue.MessageQueue;
import com.run.common.result.Page;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.F;
import com.run.dao.entity.Application;
import com.run.dao.entity.Conversation;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.mapper.ApplicationMapper;
import com.run.dao.mapper.ConversationMapper;
import com.run.dao.mapper.ConversationMessageMapper;
import com.run.handler.application.vo.ConversationVO;
import com.run.handler.conversation.IConversationHandler;
import com.run.handler.conversation.dto.ConversationProfileDTO;
import com.run.handler.conversation.vo.AnonymousLoginVO;
import com.run.handler.conversation.vo.ModifyConversationNameVO;
import com.run.handler.conversation.vo.QueryConversationMessageVO;
import com.run.handler.conversation.vo.QueryConversationVO;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.Message;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/9  22:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConversationHandlerImpl implements IConversationHandler {


    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ApplicationMapper applicationMapper;
    private final MessageQueue<String> messageQueue;

    @Inject
    public ConversationHandlerImpl(
            ConversationMapper conversationMapper,
            ConversationMessageMapper conversationMessageMapper,
            ApplicationMapper applicationMapper,
            MessageQueue<String> messageQueue) {
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.messageQueue = messageQueue;
    }

    @Override
    public void modifyName(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        ModifyConversationNameVO modifyConversationNameVO = context.body().asPojo(ModifyConversationNameVO.class);
        if (StringUtils.isEmpty(modifyConversationNameVO.getName())) {
            context.end(Result.error("名称必填").toBuffer());
        }
        conversationMapper.update(Map.of(F.field(Conversation::getName), F.params(Conversation::getName)),
                        F.field(Conversation::getId).eq(F.params(Conversation::getId)),
                        Map.of("id", conversationId, "name", modifyConversationNameVO.getName()))
                .onSuccess(ok -> {
                    context.end(Result.success(true).toBuffer());
                }).onFailure(context::fail);
    }

    @Override
    public void config(RoutingContext context) {
        String applicationId = context.queryParams().get("applicationId");
        applicationMapper.getById(applicationId).onSuccess(application -> {
            JsonObject setting = application.getSetting();
            ConversationProfileDTO conversationProfileDTO = new ConversationProfileDTO();
            conversationProfileDTO.setName(application.getName());
            conversationProfileDTO.setIcon(application.getIcon());
            conversationProfileDTO.setAuthenticationType(setting.getString("authenticationType", "anonymous"));
            context.end(Result.success(conversationProfileDTO).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void createConversation(RoutingContext context) {
        User user = context.user();
        JsonObject jsonObject = context.body().asJsonObject();
        Conversation conversation = new Conversation(CommonUtils.uuid7(),
                UUID.fromString(user.get("applicationId")),
                jsonObject.getString("name", "新建对话"),
                ConversationExecuteConstants.CONVERSATION, new JsonObject(),
                user.get("conversationUserId"),
                ConversationUserConstants.valueOf(user.get("conversationUserType")),
                0, 0, 0, 0,
                Boolean.FALSE, LocalDateTime.now(), LocalDateTime.now());
        conversationMapper.save(conversation)
                .onSuccess(_ -> context.end(Result.success(conversation).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void conversation(RoutingContext context) {
        User user = context.user();
        String applicationId = user.get("applicationId");
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
        messageQueue.create(conversationId.toString());
        AtomicLong index = new AtomicLong(1);
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
                messageQueue.complete(conversationId.toString());
                messageQueue.delete(conversationId.toString());
                return;
            }
            Message message = new Message(List.of(chunk), index.getAndIncrement());
            String messageString = "data: " + JacksonUtils.toJson(message) + "\n\n";
            messageQueue.publish(conversationId.toString(), message.index(), messageString);
            context.response().write(Buffer.buffer(messageString, "utf-8"));
        });
        workFlowManage.invoke();
    }

    @Override
    public void anonymousLogin(RoutingContext context) {
        AnonymousLoginVO anonymousLoginVO = context.body().asPojo(AnonymousLoginVO.class);
        String visitorId = anonymousLoginVO.getVisitorId();
        String applicationId = anonymousLoginVO.getApplicationId();
        ConversationTokenDTO conversationTokenDTO =
                new ConversationTokenDTO(ConversationTypeConstants.ANONYMOUS,
                        visitorId, applicationId, new JsonObject());
        context.end(Result.success(conversationTokenDTO.toToken()).toBuffer());
    }

    @Override
    public void delConversation(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        conversationMapper.update(Map.of(F.field(Conversation::getIsDeleted), F.params(Conversation::getIsDeleted)),
                        F.field(Conversation::getId).eq(F.params(Conversation::getId)),
                        Map.of("id", conversationId, "isDeleted", true))
                .onSuccess(ok -> {
                    context.end(Result.success(true).toBuffer());
                }).onFailure(context::fail);
    }

    @Override
    public void pageConversation(RoutingContext context) {
        User user = context.user();
        String conversationUserId = user.get("conversationUserId");
        QueryConversationVO queryConversationVO = Query.format(QueryConversationVO.class, context);
        conversationMapper.page(
                        F.field(Conversation::getConversationUserId).eq(F.params(Conversation::getConversationUserId))
                                .and(F.field(Conversation::getIsDeleted).eq(F.params(Conversation::getIsDeleted))),
                        List.of(F.field(Conversation::getUpdateTime)),
                        queryConversationVO.getCurrentPage(),
                        queryConversationVO.getPageSize(),
                        Map.of("conversationUserId", conversationUserId,
                                "isDeleted", Boolean.FALSE))
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageMessage(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        QueryConversationMessageVO queryConversationMessageVO = Query.format(QueryConversationMessageVO.class, context);
        conversationMessageMapper.page(
                        F.field(ConversationMessage::getConversationId).eq(F.params(ConversationMessage::getConversationId)),
                        List.of(F.field(Conversation::getUpdateTime).desc()),
                        queryConversationMessageVO.getCurrentPage(),
                        queryConversationMessageVO.getPageSize(),
                        Map.of("conversationId", conversationId)
                ).onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    public void statusStream(RoutingContext context) {
        String conversationId = context.pathParam("conversationId");
        messageQueue.exists(conversationId, ok -> {
            context.end(Result.success(Map.of("status", ok)).toBuffer());
        });
    }

    public void resumeStream(RoutingContext context) {
        String conversationId = context.pathParam("conversationId");
        context.response().setChunked(true);
        context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
        context.response().putHeader("Cache-Control", "no-cache");
        context.response().putHeader("Character-Encoding", "utf-8");
        context.response().write(Buffer.buffer("", "utf-8"));
        String header = context.request().getHeader("Last-Event-ID");
        messageQueue.exists(conversationId, ok -> {
            if (ok) {
                Thread.startVirtualThread(() -> {
                    long index;
                    try {
                        index = Long.parseLong(header);
                    } catch (NumberFormatException e) {
                        index = 0L;
                    }
                    messageQueue.consumer(conversationId, UUID.randomUUID().toString(), index, chunk -> {
                                context.response().write(Buffer.buffer(chunk, "utf-8"));
                            },
                            () -> {
                                context.response().end();
                            });
                });

            }
        });

    }
}
