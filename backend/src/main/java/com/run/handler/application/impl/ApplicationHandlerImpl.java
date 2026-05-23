package com.run.handler.application.impl;


import com.run.auth.constants.PermissionConstants;
import com.run.auth.constants.TokenTypeConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.queue.MessageQueue;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.ConversationWorkflowExecutor;
import com.run.common.util.JacksonUtils;
import com.run.common.util.TreeUtil;
import com.run.dao.entity.*;
import com.run.dao.mapper.*;
import com.run.handler.application.IApplicationHandler;
import com.run.handler.application.dto.ConversationDTO;
import com.run.handler.application.pojo.ConversationQuery;
import com.run.handler.application.pojo.EditApplicationPojo;
import com.run.handler.application.vo.ConversationVO;
import com.run.handler.application.vo.CreateApplicationVO;
import com.run.handler.application.vo.CreateConversationVO;
import com.run.handler.common.Tool;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.conversation.vo.ModifyConversationNameVO;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeSerialize;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.ContentConverter;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.Message;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static com.run.sql.DSL.field;
import static com.run.sql.DSL.param;

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
    private final MessageQueue<String> messageQueue;
    private final ConversationWorkflowExecutor executor;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper,
                                  ApplicationFolderMapper applicationFolderMapper,
                                  ApplicationRelationMapper applicationRelationMapper,
                                  ApplicationPermissionMapper applicationPermissionMapper,
                                  ConversationMapper conversationMapper,
                                  ConversationMessageMapper conversationMessageMapper,
                                  CacheStore cacheStore,
                                  MessageQueue<String> messageQueue) {
        super(applicationMapper, applicationFolderMapper, applicationRelationMapper, applicationPermissionMapper, cacheStore);
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.messageQueue = messageQueue;
        executor = new ConversationWorkflowExecutor(messageQueue, conversationMessageMapper);
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditApplicationPojo pojo = context.body().asPojo(EditApplicationPojo.class);

        Application application = new Application();
        application.setId(UUID.fromString(resourceId));
        JsonObject workflow = pojo.getWorkflow();
        if (workflow != null) {
            application.setWorkflow(workflow);
        }
        String desc = pojo.getDesc();
        if (StringUtils.isNotEmpty(desc)) {
            application.setDesc(desc);
        }
        if (StringUtils.isNotEmpty(pojo.getIcon())) {
            application.setIcon(pojo.getIcon());
        }
        if (pojo.getAllowAnonymousAccess() != null) {
            application.setAllowAnonymousAccess(pojo.getAllowAnonymousAccess());
        }
        if (!StringUtils.isBlank(pojo.getName())) {
            application.setName(pojo.getName());
            resourceMapper.getById(resourceId)
                    .compose(resource -> Tool.validateNodeName(resourceMapper, getParentId(resource), pojo.getName(), UUID.fromString(resourceId))
                            .compose(_ -> {
                                return applicationMapper.update(application);
                            })).onSuccess(ok -> {
                        context.end(Result.success(true).toBuffer());
                    }).onFailure(context::fail);
        } else {
            applicationMapper.update(application).onSuccess(ok -> {
                context.end(Result.success(true).toBuffer());
            }).onFailure(context::fail);
        }
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
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.APPLICATION_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
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
        CreateApplicationVO createApplicationVO = context.body().asPojo(CreateApplicationVO.class);
        return new Application(resourceId, parentUuId, createApplicationVO.getName(), createApplicationVO.getDesc(), createApplicationVO.getIcon(), createApplicationVO.getWorkflow(), new JsonObject(), false, false, createApplicationVO.getAllowAnonymousAccess(), LocalDateTime.now(), LocalDateTime.now());
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
        ConversationExecuteConstants executeType = conversationVO.executeType() != null
                ? conversationVO.executeType() : ConversationExecuteConstants.DEBUG;
        Conversation conversation = new Conversation(UUID.randomUUID(),
                UUID.fromString(applicationId),
                conversationVO.name(),
                executeType, new JsonObject(),
                user.getId().toString(),
                TokenTypeConstants.USER,
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
                        UUID.fromString(conversationVO.getWorkflowRunId()), ok.resultAt(1), content))
                .onFailure(context::fail);
    }

    public DefaultKeyValue<Function<WorkFlow, Node>, Map<String, Map<String, Object>>> get(Content content, JsonArray context) {
        if (content.getPosition() != null) {
            HashMap<String, Map<String, Object>> _context = new HashMap<>();
            for (int i = 0; i < context.size(); i++) {
                NodeSerialize nodeSerialize = context.getJsonObject(i).mapTo(NodeSerialize.class);
                _context.put(nodeSerialize.getNodeInfo().getId(), nodeSerialize.getContext().getMap());
            }
            return new DefaultKeyValue<>(wm -> wm.getNode(content.getPosition().id()), _context);
        } else {
            return new DefaultKeyValue<>((wm) -> wm.getNode("start-node"), new HashMap<>());
        }
    }

    private void extracted(RoutingContext context,
                           JsonObject workflow,
                           UUID conversationId,
                           UUID applicationId,
                           UUID workflowRunId,
                           List<ConversationMessage> conversationMessages,
                           Content question) {
        executor.executeWithQuestion(context, workflow, conversationId,
                applicationId, workflowRunId, conversationMessages, question);
    }

    @Override
    public void exportApplication(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        applicationMapper.getById(resourceId).onSuccess(application -> {
            JsonObject exportData = new JsonObject()
                    .put("name", application.getName())
                    .put("desc", application.getDesc())
                    .put("icon", application.getIcon())
                    .put("workflow", application.getWorkflow())
                    .put("setting", application.getSetting());
            String fileName = application.getName() + ".json";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            context.response()
                    .putHeader("Content-Type", "application/json;charset=utf-8")
                    .putHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName)
                    .end(exportData.encodePrettily());
        }).onFailure(context::fail);
    }

    @Override
    public void importApplication(RoutingContext context) {
        String folderId = context.pathParam("folderId");
        List<io.vertx.ext.web.FileUpload> uploads = context.fileUploads();
        if (uploads.isEmpty()) {
            context.fail(400);
            return;
        }
        io.vertx.ext.web.FileUpload upload = uploads.getFirst();
        try {
            String content = Files.readString(Path.of(upload.uploadedFileName()));
            JsonObject importData = new JsonObject(content);
            UUID resourceId = UUID.randomUUID();
            UUID parentUuId = TreeUtil.getParentUuId(folderId);
            String name = importData.getString("name", "导入应用");
            Application application = new Application(
                    resourceId, parentUuId, name,
                    importData.getString("desc", ""),
                    importData.getString("icon", ""),
                    importData.getJsonObject("workflow", new JsonObject()),
                    importData.getJsonObject("setting", new JsonObject()),
                    false, false, Boolean.TRUE,
                    LocalDateTime.now(), LocalDateTime.now());
            Tool.getNodeRelation(relationMapper, parentUuId, resourceId, this::newRelation, this::getAncestorId, this::getDepth)
                    .compose(relationMapper::batch_save)
                    .compose(_ -> applicationMapper.save(application))
                    .onSuccess(_ -> context.end(Result.success(application).toBuffer()))
                    .onFailure(context::fail);
        } catch (Exception e) {
            context.fail(e);
        }
    }

    public Condition getConversationQuery(ConversationQuery query) {
        Condition condition = DSL.field(Conversation::getApplicationId).eq(query.getApplicationId());
        String startTime = query.getStartTime();
        if (StringUtils.isNotEmpty(startTime)) {
            condition = condition.and(DSL.field("create_time").le(startTime));
        }
        if (StringUtils.isNotEmpty(query.getEndTime())) {
            condition = condition.and(DSL.field("create_time").ge(startTime));
        }
        if (StringUtils.isNotEmpty(query.getName())) {
            condition = condition.and(DSL.field("name").like("%" + query.getName() + "%"));
        }
        if (StringUtils.isNotEmpty(query.getExecuteType())) {
            condition = condition.and(DSL.field("execute_type").eq(query.getExecuteType()));
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
                        List.of(field(Conversation::getUpdateTime).desc()),
                        Long.parseLong(currentPage), Long.parseLong(pageSize),
                        Map.of())
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void pageConversationMessage(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String conversationId = context.pathParam("conversationId");
        String currentPage = context.queryParams().get("currentPage");
        String pageSize = context.queryParams().get("pageSize");
        conversationMessageMapper.page(field(ConversationMessage::getApplicationId).eq(applicationId)
                                .and(field(ConversationMessage::getConversationId).eq(conversationId)),
                        List.of(field(Conversation::getUpdateTime).desc()),
                        Long.parseLong(currentPage),
                        Long.parseLong(pageSize),
                        Map.of()
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

    public void cancel(RoutingContext context) {
        String conversationId = context.pathParam("conversationId");
        Thread.startVirtualThread(() -> {
            WorkflowRunRegistry.cancel(conversationId);
            context.end(Result.success(Boolean.TRUE).toBuffer());
        });

    }

    public void overview(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String daysParam = context.queryParams().get("days");
        int days = 7;
        if (StringUtils.isNotEmpty(daysParam)) {
            try {
                days = Integer.parseInt(daysParam);
            } catch (NumberFormatException ignored) {
            }
        }
        String startTime = LocalDateTime.now().minusDays(days).toLocalDate().toString();

        var client = conversationMessageMapper.getClient();

        // 1. 统计卡片
        Future<Row> statsFuture = SqlTemplate.forQuery(client,
                        "SELECT " +
                                "(SELECT COUNT(*) FROM conversation WHERE application_id = #{applicationId} AND is_deleted = 0) AS conversation_count, " +
                                "(SELECT COUNT(*) FROM conversation_message WHERE application_id = #{applicationId}) AS message_count, " +
                                "(SELECT COALESCE(SUM(prompt_tokens + completion_tokens), 0) FROM conversation_message WHERE application_id = #{applicationId}) AS total_tokens, " +
                                "(SELECT COALESCE(AVG(duration), 0) FROM conversation_message WHERE application_id = #{applicationId} AND type = 'ASSISTANT') AS avg_duration")
                .execute(Map.of("applicationId", applicationId))
                .map(rows -> rows.iterator().next());

        // 2. 消息趋势
        Future<List<JsonObject>> messageTrendFuture = SqlTemplate.forQuery(client,
                        "SELECT DATE(create_time) AS date, COUNT(*) AS count " +
                                "FROM conversation_message " +
                                "WHERE application_id = #{applicationId} AND create_time >= #{startTime} " +
                                "GROUP BY DATE(create_time) ORDER BY date")
                .execute(Map.of("applicationId", applicationId, "startTime", startTime))
                .map(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(new JsonObject()
                                .put("date", row.getString("date"))
                                .put("count", row.getLong("count")));
                    }
                    return list;
                });

        // 3. Token 趋势
        Future<List<JsonObject>> tokenTrendFuture = SqlTemplate.forQuery(client,
                        "SELECT DATE(create_time) AS date, COALESCE(SUM(prompt_tokens + completion_tokens), 0) AS total " +
                                "FROM conversation_message " +
                                "WHERE application_id = #{applicationId} AND create_time >= #{startTime} " +
                                "GROUP BY DATE(create_time) ORDER BY date")
                .execute(Map.of("applicationId", applicationId, "startTime", startTime))
                .map(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(new JsonObject()
                                .put("date", row.getString("date"))
                                .put("total", row.getLong("total")));
                    }
                    return list;
                });

        // 4. 最近提问
        Future<List<JsonObject>> recentQuestionsFuture = SqlTemplate.forQuery(client,
                        "SELECT content, create_time " +
                                "FROM conversation_message " +
                                "WHERE application_id = #{applicationId} AND type = 'USER' " +
                                "ORDER BY create_time DESC LIMIT 5")
                .execute(Map.of("applicationId", applicationId))
                .map(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(new JsonObject()
                                .put("content", row.getString("content"))
                                .put("createTime", row.getLocalDateTime("create_time").toString()));
                    }
                    return list;
                });

        Future.all(statsFuture, messageTrendFuture, tokenTrendFuture, recentQuestionsFuture)
                .onSuccess(ok -> {
                    Row stats = ok.resultAt(0);
                    JsonObject result = new JsonObject()
                            .put("conversationCount", stats.getLong("conversation_count"))
                            .put("messageCount", stats.getLong("message_count"))
                            .put("totalTokens", stats.getLong("total_tokens"))
                            .put("avgDuration", stats.getLong("avg_duration"))
                            .put("messageTrend", ok.<List<JsonObject>>resultAt(1))
                            .put("tokenTrend", ok.<List<JsonObject>>resultAt(2))
                            .put("recentQuestions", ok.<List<JsonObject>>resultAt(3));
                    context.end(Result.success(result).toBuffer());
                })
                .onFailure(context::fail);
    }

    @Override
    public void mineConversation(RoutingContext context) {
        String userId = context.user().get("id");
        String currentPage = context.queryParams().get("currentPage");
        String pageSize = context.queryParams().get("pageSize");
        MultiMap entries = context.queryParams().copy();
        entries.addAll(context.pathParams());
        ConversationQuery conversation = new ConversationQuery(entries);
        Condition conversationQuery = getConversationQuery(conversation);
        conversationQuery.and(field(Conversation::getConversationUserId).eq(userId));
        conversationMapper.page(conversationQuery.and(field(Conversation::getIsDeleted).eq(Boolean.FALSE)),
                        List.of(field(Conversation::getUpdateTime).desc()),
                        Long.parseLong(currentPage), Long.parseLong(pageSize),
                        Map.of())
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    public void modifyConversationName(RoutingContext context) {
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

    public void deleteConversation(RoutingContext context) {
        String conversationId = context.pathParams().get("conversationId");
        conversationMapper.update(Map.of(field(Conversation::getIsDeleted), param(Conversation::getIsDeleted, Boolean.TRUE)),
                        field(Conversation::getId).eq(conversationId)
                )
                .onSuccess(ok -> {
                    context.end(Result.success(true).toBuffer());
                }).onFailure(context::fail);
    }
}
