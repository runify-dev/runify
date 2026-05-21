package com.run.handler.conversation.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.constants.TokenTypeConstants;
import com.run.auth.dto.TokenDTO;
import com.run.auth.provider.TokenProvider;
import com.run.common.apispec.ApiSpec;
import com.run.common.apispec.SchemaBuilder;
import com.run.common.apispec.ValidationException;
import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.ConversationUserConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.query.Query;
import com.run.common.queue.MessageQueue;
import com.run.common.result.Page;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;

import com.run.dao.entity.*;
import com.run.dao.mapper.*;
import com.run.handler.application.vo.ConversationVO;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.QueryResourcePojo;
import com.run.handler.conversation.IConversationHandler;
import com.run.handler.conversation.dto.ApplicationDTO;
import com.run.handler.conversation.dto.ConversationProfileDTO;
import com.run.handler.conversation.dto.UserProfileDTO;
import com.run.handler.conversation.vo.*;
import com.run.handler.user.dto.UserDTO;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import com.run.workflow.*;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.ContentConverter;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.Message;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.run.common.util.ConditionCommonUtil.isApplicationRead;
import static com.run.sql.DSL.field;
import static com.run.sql.DSL.param;

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
    private final UserMapper userMapper;
    private final MessageQueue<String> messageQueue;
    private final ApplicationRelationMapper applicationRelationMapper;
    private final ApplicationPermissionMapper applicationPermissionMapper;
    private final RolePermissionRelationMapper rolePermissionRelationMapper;
    private final RoleUserRelationMapper roleUserRelationMapper;
    private final RoleMapper roleMapper;

    @Inject
    public ConversationHandlerImpl(
            ConversationMapper conversationMapper,
            ConversationMessageMapper conversationMessageMapper,
            ApplicationMapper applicationMapper,
            ApplicationRelationMapper applicationRelationMapper,
            ApplicationPermissionMapper applicationPermissionMapper,
            UserMapper userMapper,
            RolePermissionRelationMapper rolePermissionRelationMapper,
            RoleUserRelationMapper roleUserRelationMapper,
            RoleMapper roleMapper,
            MessageQueue<String> messageQueue) {
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.messageQueue = messageQueue;
        this.userMapper = userMapper;
        this.applicationRelationMapper = applicationRelationMapper;
        this.applicationPermissionMapper = applicationPermissionMapper;
        this.rolePermissionRelationMapper = rolePermissionRelationMapper;
        this.roleUserRelationMapper = roleUserRelationMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public void modifyName(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        ModifyConversationNameVO modifyConversationNameVO = context.body().asPojo(ModifyConversationNameVO.class);
        if (StringUtils.isEmpty(modifyConversationNameVO.getName())) {
            context.end(Result.error("名称必填").toBuffer());
        }
        conversationMapper.update(Map.of(field(Conversation::getName), param("name")),
                        field(Conversation::getId).eq(conversationId),
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
        String applicationId = context.pathParam("applicationId");
        JsonObject jsonObject = context.body().asJsonObject();
        Conversation conversation = new Conversation(CommonUtils.uuid7(),
                UUID.fromString(applicationId),
                jsonObject.getString("name", "新建对话"),
                ConversationExecuteConstants.CONVERSATION, new JsonObject(),
                user.get("conversationUserId"),
                TokenTypeConstants.valueOf(user.get("conversationUserType")),
                0, 0, 0, 0,
                Boolean.FALSE, LocalDateTime.now(), LocalDateTime.now());
        conversationMapper.save(conversation)
                .onSuccess(_ -> context.end(Result.success(conversation).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void conversation(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String conversationId = context.pathParam("conversationId");
        ConversationVO conversationVO = context.body().asPojo(ConversationVO.class);
        Content content = ContentConverter.of(conversationVO.getContent(), conversationVO.getWorkflowRunId());
        ConversationMessage conversationMessage = new ConversationMessage(UUID.randomUUID(),
                UUID.fromString(conversationId),
                UUID.fromString(applicationId), UUID.fromString(conversationVO.getWorkflowRunId()),
                MessageConstants.USER,
                new JsonArray(List.of(content)),
                new JsonArray(),
                0,
                0,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now());

        Future<Application> applicationFuture = applicationMapper.getById(applicationId);

        Future<List<ConversationMessage>> conversationMessageFuture = conversationMessageMapper.save(conversationMessage)
                .compose(ok -> conversationMessageMapper
                        .list(conversationMessageMapper.select().where(field(ConversationMessage::getConversationId)
                                        .eq(conversationId))
                                .orderBy(field(ConversationMessage::getCreateTime).desc())
                                .limit(10).render()));
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
        try {
            List<ConversationMessage> list = conversationMessages.stream().sorted(Comparator.comparing(ConversationMessage::getCreateTime)).toList();
            context.response().setChunked(true);
            context.response().putHeader("Content-Type", "text/event-stream;charset=utf-8");
            context.response().putHeader("Cache-Control", "no-cache");
            context.response().putHeader("Character-Encoding", "utf-8");
            context.response().write(Buffer.buffer("", "utf-8"));
            messageQueue.create(conversationId.toString());
            AtomicLong index = new AtomicLong(1);
            Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId, "_tool_state");
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            WorkFlowManage workFlowManage = new WorkFlowManage(WorkFlow.of(workflow, WorkflowType.CHAT_WORKFLOW),
                    new HashMap<>(Map.of(
                            "messages", list,
                            "conversationId", conversationId,
                            "applicationId", applicationId)),
                    new HashMap<>(), (wm, node, chunk, isEnd) -> {
                if (isEnd) {
                    WorkflowRunRegistry.unregister(conversationId.toString());
                    messageQueue.complete(conversationId.toString());
                    messageQueue.delete(conversationId.toString());
                    List<Content> chunks = wm.getChunks();
                    List<ConversationMessage> messageArrayList = new ArrayList<>();
                    List<INode<?, ?>> nodes = wm.getNodes();
                    int promptTokens = wm.getPromptTokens();
                    int completionTokens = wm.getCompletionTokens();
                    long runtime = wm.getRuntime();
                    wm.clear();
                    ConversationMessage conversationMessage = new ConversationMessage(UUID.randomUUID(),
                            conversationId,
                            applicationId, workflowRunId,
                            MessageConstants.ASSISTANT,
                            new JsonArray(chunks),
                            new JsonArray(nodes.stream().map(INode::serialize).toList()),
                            promptTokens,
                            completionTokens,
                            runtime,
                            LocalDateTime.now(),
                            LocalDateTime.now());
                    messageArrayList.add(conversationMessage);
                    conversationMessageMapper.batch_save(messageArrayList)
                            .onSuccess(_ -> {
                                context.response().end();
                            })
                            .onFailure(context::fail);

                    return;
                }
                Message message = new Message(List.of(chunk), index.getAndIncrement());
                String messageString = "data: " + JacksonUtils.toJson(message) + "\n\n";
                messageQueue.publish(conversationId.toString(), message.index(), messageString);
                context.response().write(Buffer.buffer(messageString, "utf-8"));
            });
            workFlowManage.invoke();
        } catch (Exception e) {
            Message message = new Message(List.of(new FailureContent(e.getMessage(), workflowRunId.toString(), CommonUtils.uuid7().toString(), NodeStatus.FAIL, "", "")), 0);
            String messageString = "data: " + JacksonUtils.toJson(message) + "\n\n";
            context.end(messageString);
            messageQueue.complete(conversationId.toString());
            WorkflowRunRegistry.unregister(workflowRunId.toString());
        }
    }

    @Override
    public void anonymousLogin(RoutingContext context) {
        AnonymousLoginVO anonymousLoginVO = context.body().asPojo(AnonymousLoginVO.class);
        String visitorId = anonymousLoginVO.getVisitorId();
        UUID uuid = UUID.nameUUIDFromBytes(visitorId.getBytes());
        TokenDTO tokenDTO =
                new TokenDTO(TokenTypeConstants.ANONYMOUS,
                        uuid.toString(), new JsonObject());
        context.end(Result.success(tokenDTO.toToken()).toBuffer());
    }

    @Override
    public void delConversation(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        conversationMapper.update(Map.of(field(Conversation::getIsDeleted), param(Conversation::getIsDeleted, Boolean.TRUE)),
                        field(Conversation::getId).eq(conversationId)
                )
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
                        field(Conversation::getConversationUserId).eq(conversationUserId)
                                .and(field(Conversation::getIsDeleted).eq(Boolean.FALSE)),
                        List.of(field(Conversation::getUpdateTime).asc()),
                        queryConversationVO.getCurrentPage(),
                        queryConversationVO.getPageSize(), Map.of())
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageMessage(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        QueryConversationMessageVO queryConversationMessageVO = Query.format(QueryConversationMessageVO.class, context);
        conversationMessageMapper.page(
                        field(ConversationMessage::getConversationId).eq(conversationId),
                        List.of(field(Conversation::getUpdateTime).desc()),
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

    @Override
    public void cancel(RoutingContext context) {
        String conversationId = context.pathParam("conversationId");
        Thread.startVirtualThread(() -> {
            WorkflowRunRegistry.cancel(conversationId);
            context.end(Result.success(Boolean.TRUE).toBuffer());
        });

    }


    private Condition getConditionSync(ApplicationQueryVO query) {
        Condition condition = field(Application::getAllowAnonymousAccess).eq(Boolean.TRUE);
        if (StringUtils.isNotEmpty(query.getName())) {
            return condition.and(field(Application::getName).like("%" + query.getName() + "%"));
        }
        return condition;
    }

    private Future<Condition> getConditionAsync(ApplicationQueryVO query, User user) {
        String userId = user.get("id");
        return isApplicationRead(applicationPermissionMapper, roleMapper, roleUserRelationMapper, userId).map(isRead -> {
            QueryResourcePojo queryResourcePojo = new QueryResourcePojo();
            if (StringUtils.isNotEmpty(query.getName())) {
                queryResourcePojo.setName(query.getName());
            }
            Condition condition = ResourceHandlerImpl.getWhereByPermission(applicationPermissionMapper, applicationRelationMapper, queryResourcePojo, isRead);
            return condition.or(field(Application::getAllowAnonymousAccess).eq(Boolean.TRUE));
        });
    }

    @Override
    public void query(RoutingContext routingContext) {
        User user = routingContext.user();
        ApplicationQueryVO query = Query.format(ApplicationQueryVO.class, routingContext);
        Map<String, Object> params = Map.of("userId", user.get("id"), "permissionView", "VIEW",
                "permissionManage", "MANAGE",
                "permissionNotAuth", "NOT_AUTH",
                "permissionRole", "ROLE");

        String type = user.get("type");
        Future<Condition> conditionFuture;
        if (Strings.CS.equals(type, TokenTypeConstants.USER.name())) {
            conditionFuture = getConditionAsync(query, user);
        } else {
            conditionFuture = Future.succeededFuture(getConditionSync(query));
        }

        conditionFuture.compose(condition -> {
            if (query.getCurrentPage() != null && query.getPageSize() != null) {
                return applicationMapper.page(condition, query.getCurrentPage(), query.getPageSize(), params)
                        .map(r -> {
                            Page<ApplicationDTO> page = new Page<>();
                            page.setCurrent(r.getCurrent());
                            page.setSize(r.getSize());
                            page.setTotal(r.getTotal());
                            page.setRecords(r.getRecords().stream().map(ApplicationDTO::new).toList());
                            return (Object) page;
                        });
            } else {
                return applicationMapper.list(condition, params)
                        .map(r -> (Object) r.stream().map(ApplicationDTO::new).toList());
            }
        }).onSuccess(result -> {
            routingContext.end(Result.success(result).toBuffer());
        }).onFailure(routingContext::fail);
    }

    @Override
    public void login(RoutingContext context) {
        ApiSpec spec = ApiSpec.builder()
                .body(SchemaBuilder.objectBuilder()
                        .required("username", SchemaBuilder.string().minLength(0, "用户名必填").build(), "名称不能为空")
                        .property("password", SchemaBuilder.string().minLength(0, "秘密必填").build())
                        .build())
                .build();
        List<ValidationException.ValidationError> validationErrors = spec.validateQuietly(context);
        if (!validationErrors.isEmpty()) {
            context.end(Result.error(validationErrors.stream().findFirst().toString()).toBuffer());
            return;
        }
        JsonObject jsonObject = context.body().asJsonObject();
        String username = jsonObject.getString("username");
        String password = jsonObject.getString("password");
        Condition eq = field(com.run.dao.entity.User::getUsername)
                .eq(username)
                .and(field(com.run.dao.entity.User::getPassword).eq(CommonUtils.getSHA256(password)));
        userMapper.search(eq, Map.of())
                .onSuccess(rows -> {
                    if (rows.size() > 0) {
                        com.run.dao.entity.User user = rows.iterator().next();
                        TokenDTO tokenDTO = new TokenDTO(TokenTypeConstants.USER, user.getId().toString(), new JsonObject());
                        context.end(Result.success(tokenDTO.toToken()).toBuffer());
                    } else {
                        context.end(Result.error("用户名或者密码错误").toBuffer());
                    }
                }).onFailure(e -> {
                    context.end(Result.error(e.toString()).toBuffer());
                });
    }

    @Override
    public void userProfile(RoutingContext context) {
        User user = context.user();
        TokenTypeConstants type = TokenTypeConstants.valueOf(user.get("type"));
        UserProfileDTO userProfileDTO = new UserProfileDTO();
        userProfileDTO.setId(user.get("id"));
        userProfileDTO.setType(type);
        if (type.equals(TokenTypeConstants.USER)) {
            userMapper.getById(user.get("id")).onSuccess(u -> {
                userProfileDTO.setUser(new UserDTO(u));
                context.end(Result.success(userProfileDTO).toBuffer());
            }).onFailure(context::fail);
        } else {
            context.end(Result.success(userProfileDTO).toBuffer());
        }

    }

    @Override
    public void application(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        applicationMapper.getById(applicationId).onSuccess(a -> {
            context.end(Result.success(new ApplicationDTO(a)).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void authProfile(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        applicationMapper.getById(applicationId).onSuccess(a -> {
            Boolean allowAnonymousAccess = a.getAllowAnonymousAccess();
            context.end(Result.success(Map.of("allowAnonymousAccess", allowAnonymousAccess)).toBuffer());
        }).onFailure(context::fail);
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

            } else {
                context.response().end();
            }
        });

    }
}
