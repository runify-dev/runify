package com.run.handler.application.impl;


import com.run.auth.constants.PermissionConstants;
import com.run.auth.constants.TokenTypeConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.config.AppConfig;
import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.MessageConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.queue.MessageQueue;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.ConversationUser;
import com.run.common.util.ConversationWorkflowExecutor;
import com.run.common.util.JacksonUtils;
import com.run.common.util.TreeUtil;
import com.run.dao.common.convert.EntityConvert;
import com.run.dao.common.convert.postgres.PostgresConvert;
import com.run.dao.common.convert.sqlite.SqliteConvert;
import com.run.dao.entity.*;
import com.run.dao.mapper.*;
import com.run.handler.application.vo.MessageTrendVO;
import com.run.handler.application.vo.OverviewStatsVO;
import com.run.handler.application.vo.TokenTrendVO;
import com.run.sql.dialect.SQLDialect;
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
import com.run.sql.model.Field;
import com.run.workflow.*;
import com.run.workflow.nodes.contextmanage.service.SectionRegistry;
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
    private final AppConfig appConfig;
    private final com.run.dao.mapper.CtxSummaryMapper ctxSummaryMapper;
    private final com.run.dao.mapper.CtxFactMapper ctxFactMapper;

    @Inject
    public ApplicationHandlerImpl(ApplicationMapper applicationMapper,
                                  ApplicationFolderMapper applicationFolderMapper,
                                  ApplicationRelationMapper applicationRelationMapper,
                                  ApplicationPermissionMapper applicationPermissionMapper,
                                  ConversationMapper conversationMapper,
                                  ConversationMessageMapper conversationMessageMapper,
                                  CacheStore cacheStore,
                                  MessageQueue<String> messageQueue,
                                  AppConfig appConfig,
                                  com.run.dao.mapper.CtxSummaryMapper ctxSummaryMapper,
                                  com.run.dao.mapper.CtxFactMapper ctxFactMapper) {
        super(applicationMapper, applicationFolderMapper, applicationRelationMapper, applicationPermissionMapper, cacheStore);
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.messageQueue = messageQueue;
        this.appConfig = appConfig;
        this.ctxSummaryMapper = ctxSummaryMapper;
        this.ctxFactMapper = ctxFactMapper;
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
            this.cacheStore.delete("c::" + ((User) context.user().get("user")).getId());
            application.setAllowAnonymousAccess(pojo.getAllowAnonymousAccess());
        }
        if (pojo.getAppType() != null) {
            application.setAppType(pojo.getAppType());
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
        return new Application(resourceId, parentUuId, createApplicationVO.getName(), createApplicationVO.getDesc(), createApplicationVO.getIcon(), createApplicationVO.getWorkflow(), new JsonObject(), false, false, createApplicationVO.getAllowAnonymousAccess(), createApplicationVO.getAppType(), LocalDateTime.now(), LocalDateTime.now());
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
        // 登录用户：白名单展示字段（排除 password/role 等敏感与授权字段），恒 USER 类型
        User principal = context.user().get("user");
        JsonObject profile = new JsonObject()
                .put("nickname", principal.getNickname())
                .put("username", principal.getUsername())
                .put("email", principal.getEmail())
                .put("phone", principal.getPhone())
                .put("icon", principal.getIcon());
        ConversationUser user = new ConversationUser(principal.getId().toString(),
                TokenTypeConstants.USER, profile);
        executor.executeWithQuestion(context, workflow, conversationId,
                applicationId, workflowRunId, conversationMessages, question, user);
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
                    .put("setting", application.getSetting())
                    .put("appType", application.getAppType());
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
                    importData.getString("appType"),
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

    /**
     * 会话的上下文记忆（跨对话层）：ctx_summary 摘要 + ctx_fact 便签。
     * 合并顺序与 context-query-node 注入上下文时严格一致 —— 应用级非观察期 → 用户级非观察期 → 对话级，
     * 后者覆盖同键（越具体越优先）；用户级取本会话归属用户（applicationId:userId，仅登录用户，匿名无）。
     * 产物是 subtype=artifact 的便签。
     */
    @Override
    public void conversationContext(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        String conversationId = context.pathParam("conversationId");

        conversationMapper.getById(conversationId)
                .compose(conversation -> loadConversationMemory(applicationId, conversationId, conversation))
                .onSuccess(result -> context.end(Result.success(result).toBuffer()))
                .onFailure(context::fail);
    }

    /**
     * 载入并组装某会话的记忆（摘要 + 三 scope 合并便签）。userScopeId 与 save/query 节点同规则。
     */
    private Future<JsonObject> loadConversationMemory(String applicationId, String conversationId,
                                                      Conversation conversation) {
        String userScopeId = userScopeId(applicationId, conversation);

        Future<com.run.dao.entity.CtxSummary> summaryFuture = ctxSummaryMapper.one(
                field(com.run.dao.entity.CtxSummary::getScopeType).eq("conversation")
                        .and(field(com.run.dao.entity.CtxSummary::getScopeId).eq(conversationId)), Map.of());
        Future<List<com.run.dao.entity.CtxFact>> appFactsFuture = ctxFactMapper.list(
                field(com.run.dao.entity.CtxFact::getScopeType).eq("application")
                        .and(field(com.run.dao.entity.CtxFact::getScopeId).eq(applicationId)), Map.of());
        Future<List<com.run.dao.entity.CtxFact>> userFactsFuture = StringUtils.isBlank(userScopeId)
                ? Future.succeededFuture(List.of())
                : ctxFactMapper.list(field(com.run.dao.entity.CtxFact::getScopeType).eq("user")
                        .and(field(com.run.dao.entity.CtxFact::getScopeId).eq(userScopeId))
                        .and(field(com.run.dao.entity.CtxFact::getApplicationId).eq(applicationId)), Map.of());
        Future<List<com.run.dao.entity.CtxFact>> convFactsFuture = ctxFactMapper.list(
                field(com.run.dao.entity.CtxFact::getScopeType).eq("conversation")
                        .and(field(com.run.dao.entity.CtxFact::getScopeId).eq(conversationId)), Map.of());

        return Future.all(summaryFuture, appFactsFuture, userFactsFuture, convFactsFuture).map(composite -> {
            com.run.dao.entity.CtxSummary summaryRow = composite.resultAt(0);
            List<com.run.dao.entity.CtxFact> appFacts = composite.resultAt(1);
            List<com.run.dao.entity.CtxFact> userFacts = composite.resultAt(2);
            List<com.run.dao.entity.CtxFact> convFacts = composite.resultAt(3);

            Map<String, com.run.dao.entity.CtxFact> merged = new LinkedHashMap<>();
            for (com.run.dao.entity.CtxFact fact : appFacts) {
                if (!Boolean.TRUE.equals(fact.getProvisional())) {
                    merged.put(fact.getSubtype() + "|" + fact.getFactKey(), fact);
                }
            }
            for (com.run.dao.entity.CtxFact fact : userFacts) {
                if (!Boolean.TRUE.equals(fact.getProvisional())) {
                    merged.put(fact.getSubtype() + "|" + fact.getFactKey(), fact);
                }
            }
            for (com.run.dao.entity.CtxFact fact : convFacts) {
                merged.put(fact.getSubtype() + "|" + fact.getFactKey(), fact);
            }

            JsonObject summary = new JsonObject();
            if (summaryRow != null) {
                summary.put("text", summaryRow.getSummaryText())
                        .put("coveredUpto", Objects.toString(summaryRow.getCoveredUpto(), null))
                        .put("updateTime", Objects.toString(summaryRow.getUpdateTime(), null));
            }
            JsonArray facts = new JsonArray();
            for (com.run.dao.entity.CtxFact fact : merged.values()) {
                facts.add(new JsonObject()
                        .put("subtype", fact.getSubtype())
                        .put("key", fact.getFactKey())
                        .put("value", fact.getFactValue())
                        .put("scopeType", fact.getScopeType())
                        .put("updateTime", Objects.toString(fact.getUpdateTime(), null)));
            }
            return new JsonObject()
                    .put("summary", summary)
                    .put("facts", facts);
        });
    }

    /**
     * user 档 scope_id = 裸 userId；per-app 隔离由查询追加的 application_id 条件承担。
     * 仅登录用户（type=USER）成立，匿名/无身份返回 null。与 context-save/query 节点规则严格一致，
     * 保证读到的正是注入对话的那份。
     */
    private String userScopeId(String applicationId, Conversation conversation) {
        if (StringUtils.isBlank(applicationId) || conversation == null
                || conversation.getConversationUserType() != TokenTypeConstants.USER) {
            return null;
        }
        String userId = conversation.getConversationUserId();
        return StringUtils.isBlank(userId) ? null : userId;
    }

    /**
     * 便签设置：列出该应用的便签子区配置。
     * 首次访问（库中无该应用的行）时把内置默认子区落库，之后即为普通可编辑/可删除记录——
     * 不再靠前端写死"内置清单"做特判，内置默认只作为一次性种子。
     */
    @Override
    public void listSections(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        var mapper = com.run.RunApplication.appComponent.ctxSectionMapper();
        mapper.list(field(com.run.dao.entity.CtxSection::getApplicationId).eq(applicationId), Map.of())
                .compose(rows -> {
                    if ((rows != null && !rows.isEmpty()) || StringUtils.isBlank(applicationId)) {
                        return io.vertx.core.Future.succeededFuture(rows);
                    }
                    LocalDateTime now = LocalDateTime.now();
                    List<com.run.dao.entity.CtxSection> seed = new ArrayList<>();
                    for (com.run.workflow.nodes.contextmanage.service.SectionRegistry.Section s
                            : com.run.workflow.nodes.contextmanage.service.SectionRegistry.BUILTIN_DEFAULTS) {
                        seed.add(new com.run.dao.entity.CtxSection(CommonUtils.uuid7(), applicationId,
                                s.key(), s.label(), s.description(), s.scope(),
                                s.listStyle(), s.enabled(), s.sortOrder(), now, now));
                    }
                    // 并发首访可能同时播种：失败（唯一键冲突）则回读一次，拿到已落库的行
                    return mapper.batch_save(seed).map(ok -> seed)
                            .recover(err -> mapper.list(
                                    field(com.run.dao.entity.CtxSection::getApplicationId).eq(applicationId), Map.of()));
                })
                .onSuccess(rows -> {
                    JsonArray arr = new JsonArray();
                    for (com.run.workflow.nodes.contextmanage.service.SectionRegistry.Section s
                            : com.run.workflow.nodes.contextmanage.service.SectionRegistry.fromRows(rows)) {
                        arr.add(new JsonObject()
                                .put("sectionKey", s.key())
                                .put("label", s.label())
                                .put("description", s.description())
                                .put("scope", s.scope())
                                .put("listStyle", s.listStyle())
                                .put("enabled", s.enabled())
                                .put("sortOrder", s.sortOrder()));
                    }
                    context.end(Result.success(arr).toBuffer());
                })
                .onFailure(context::fail);
    }

    /**
     * 便签设置：整表替换该应用的便签子区配置（删旧 + 批量插新，一次落全量编辑/增删/排序）
     */
    @Override
    public void saveSections(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        JsonArray body = context.body().asJsonArray();
        java.util.List<com.run.dao.entity.CtxSection> rows = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (body != null) {
            for (int i = 0; i < body.size(); i++) {
                JsonObject o = body.getJsonObject(i);
                String key = o.getString("sectionKey");
                if (key == null || key.isBlank()) {
                    continue;
                }
                rows.add(new com.run.dao.entity.CtxSection(CommonUtils.uuid7(), applicationId, key,
                        o.getString("label"), o.getString("description"),
                        o.getString("scope", "conversation"),
                        o.getBoolean("listStyle", false), o.getBoolean("enabled", true),
                        o.getInteger("sortOrder", 0), now, now));
            }
        }
        var mapper = com.run.RunApplication.appComponent.ctxSectionMapper();
        mapper.delete(field(com.run.dao.entity.CtxSection::getApplicationId).eq(applicationId), Map.of())
                .compose(ok -> rows.isEmpty()
                        ? io.vertx.core.Future.succeededFuture()
                        : mapper.batch_save(rows))
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    /**
     * 后台侧「我的便签」：当前登录管理员在该应用作为 user 沉淀的 user 档便签（调试/管理端对话页用）。
     * 身份取后台登录用户；scope_id = 管理员id、application_id = applicationId —— 与调试执行时 save 落库规则一致。
     * 与终端侧 {@link ConversationHandlerImpl#mySections} 仅身份来源不同，视图组装共用 SectionRegistry。
     */
    @Override
    public void mySections(RoutingContext context) {
        String applicationId = context.pathParam("applicationId");
        User principal = context.user().get("user");
        if (StringUtils.isBlank(applicationId) || principal == null || principal.getId() == null) {
            context.end(Result.success(new JsonArray()).toBuffer());
            return;
        }
        String userId = principal.getId().toString();

        Future<List<CtxSection>> sectionFuture = com.run.RunApplication.appComponent.ctxSectionMapper().list(
                field(CtxSection::getApplicationId).eq(applicationId), Map.of());
        Future<List<CtxFact>> factFuture = ctxFactMapper.list(
                field(CtxFact::getScopeType).eq("user").and(field(CtxFact::getScopeId).eq(userId))
                        .and(field(CtxFact::getApplicationId).eq(applicationId)), Map.of());

        Future.all(sectionFuture, factFuture).onSuccess(composite ->
                context.end(Result.success(SectionRegistry.renderUserFacts(
                        composite.resultAt(0), composite.resultAt(1))).toBuffer())
        ).onFailure(context::fail);
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

    private <T> EntityConvert<T> converter(Class<T> clazz) {
        SQLDialect dialect = appConfig.getDatabase().getType();
        return dialect == SQLDialect.POSTGRESQL ? new PostgresConvert<>(clazz) : new SqliteConvert<>(clazz);
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
        LocalDateTime startTime = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();

        var dsl = conversationMessageMapper.getDslContext();
        var client = conversationMessageMapper.getClient();

        var msgTable = DSL.table("conversation_message");
        var convTable = DSL.table("conversation");
        var appId = DSL.<String>field("application_id");
        var createTime = DSL.<LocalDateTime>field("create_time");
        var typeField = DSL.<String>field("type");
        var durationField = DSL.<Long>field("duration");

        Condition appCondition = appId.eq(param("applicationId", applicationId));

        // 1. 统计卡片
        EntityConvert<OverviewStatsVO> statsConvert = converter(OverviewStatsVO.class);
        var convCountSub = dsl.select(DSL.count()).from(convTable)
                .where(appCondition.and(field(Conversation::getIsDeleted).eq(false)));
        var msgCountSub = dsl.select(DSL.count()).from(msgTable)
                .where(appCondition);
        var totalTokensSub = dsl.select(DSL.coalesce(DSL.sum(DSL.rawField("prompt_tokens + completion_tokens")), DSL.inline(0L)))
                .from(msgTable)
                .where(appCondition);
        var avgDurationSub = dsl.select(DSL.coalesce(DSL.avg(durationField), DSL.inline(0L)))
                .from(msgTable)
                .where(appCondition.and(typeField.eq("ASSISTANT")));

        var statsSql = dsl.select(
                Field.<Long>expression("conversation_count", ctx -> convCountSub.render(ctx)).as("conversation_count"),
                Field.<Long>expression("message_count", ctx -> msgCountSub.render(ctx)).as("message_count"),
                Field.<Long>expression("total_tokens", ctx -> totalTokensSub.render(ctx)).as("total_tokens"),
                Field.<Long>expression("avg_duration", ctx -> avgDurationSub.render(ctx)).as("avg_duration")
        ).render();

        Future<OverviewStatsVO> statsFuture = SqlTemplate.forQuery(client, statsSql.sql())
                .mapTo(statsConvert::mapTo)
                .execute(statsSql.params())
                .map(rows -> rows.iterator().next());

        // 2. 消息趋势
        EntityConvert<MessageTrendVO> msgTrendConvert = converter(MessageTrendVO.class);
        var msgTrendSql = dsl.select(DSL.date(createTime).as("date"), DSL.count().as("count"))
                .from(msgTable)
                .where(appCondition.and(createTime.ge(param("startTime", startTime))))
                .groupBy(DSL.date(createTime))
                .orderBy(DSL.date(createTime).asc())
                .render();

        Future<List<JsonObject>> messageTrendFuture = SqlTemplate.forQuery(client, msgTrendSql.sql())
                .mapTo(msgTrendConvert::mapTo)
                .execute(msgTrendSql.params())
                .map(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (MessageTrendVO item : rows) {
                        list.add(new JsonObject()
                                .put("date", item.getDate() == null ? null : item.getDate().toString())
                                .put("count", item.getCount()));
                    }
                    return list;
                });

        // 3. Token 趋势
        EntityConvert<TokenTrendVO> tokenTrendConvert = converter(TokenTrendVO.class);
        var tokenTrendSql = dsl.select(
                        DSL.date(createTime).as("date"),
                        DSL.coalesce(DSL.sum(DSL.<Long>rawField("prompt_tokens + completion_tokens")), DSL.inline(0L)).as("total")
                ).from(msgTable)
                .where(appCondition.and(createTime.ge(param("startTime", startTime))))
                .groupBy(DSL.date(createTime))
                .orderBy(DSL.date(createTime).asc())
                .render();

        Future<List<JsonObject>> tokenTrendFuture = SqlTemplate.forQuery(client, tokenTrendSql.sql())
                .mapTo(tokenTrendConvert::mapTo)
                .execute(tokenTrendSql.params())
                .map(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (TokenTrendVO item : rows) {
                        list.add(new JsonObject()
                                .put("date", item.getDate() == null ? null : item.getDate().toString())
                                .put("total", item.getTotal()));
                    }
                    return list;
                });

        // 4. 最近提问
        Condition recentCondition = field(ConversationMessage::getApplicationId).eq(applicationId)
                .and(field(ConversationMessage::getType).eq(MessageConstants.USER));
        Future<List<JsonObject>> recentQuestionsFuture = conversationMessageMapper.list(
                        conversationMessageMapper.select()
                                .where(recentCondition)
                                .orderBy(field(ConversationMessage::getCreateTime).desc())
                                .limit(5L)
                                .render())
                .map(messages -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (ConversationMessage msg : messages) {
                        list.add(new JsonObject()
                                .put("content", msg.getContent())
                                .put("createTime", msg.getCreateTime().toString()));
                    }
                    return list;
                });

        Future.all(statsFuture, messageTrendFuture, tokenTrendFuture, recentQuestionsFuture)
                .onSuccess(ok -> {
                    OverviewStatsVO stats = ok.resultAt(0);
                    JsonObject result = new JsonObject()
                            .put("conversationCount", stats.getConversationCount())
                            .put("messageCount", stats.getMessageCount())
                            .put("totalTokens", stats.getTotalTokens())
                            .put("avgDuration", stats.getAvgDuration())
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
