package com.run.handler.project.impl;

import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.dao.entity.ProjectAiBlueprint;
import com.run.dao.entity.ProjectAiMessage;
import com.run.dao.entity.ProjectAiSession;
import com.run.dao.entity.ProjectAiTask;
import com.run.dao.mapper.ProjectAiBlueprintMapper;
import com.run.dao.mapper.ProjectAiMessageMapper;
import com.run.dao.mapper.ProjectAiSessionMapper;
import com.run.dao.mapper.ProjectAiTaskMapper;
import com.run.handler.project.IProjectAiHandler;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.run.sql.DSL.field;

/**
 * 项目级 AI 生成持久化的统一 handler（蓝图 / 会话 / 任务 / 消息）。
 * 存取语义直白：CRUD + append-only 消息流；seq 由服务端权威分配。
 */
public class ProjectAiHandlerImpl implements IProjectAiHandler {
    private final ProjectAiBlueprintMapper blueprintMapper;
    private final ProjectAiSessionMapper sessionMapper;
    private final ProjectAiTaskMapper taskMapper;
    private final ProjectAiMessageMapper messageMapper;

    @Inject
    public ProjectAiHandlerImpl(ProjectAiBlueprintMapper blueprintMapper,
                                ProjectAiSessionMapper sessionMapper,
                                ProjectAiTaskMapper taskMapper,
                                ProjectAiMessageMapper messageMapper) {
        this.blueprintMapper = blueprintMapper;
        this.sessionMapper = sessionMapper;
        this.taskMapper = taskMapper;
        this.messageMapper = messageMapper;
    }

    /** 空 body 容错：无请求体时返回空 JsonObject，避免 asJsonObject 解码异常 */
    private JsonObject body(RoutingContext context) {
        Buffer buffer = context.body().buffer();
        if (buffer == null || buffer.length() == 0) {
            return new JsonObject();
        }
        return context.body().asJsonObject();
    }

    private void ok(RoutingContext context, Object data) {
        context.end(Result.success(data).toBuffer());
    }

    // ── L1 蓝图 ───────────────────────────────────────────────────────────

    @Override
    public void getBlueprint(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        blueprintMapper.list(field(ProjectAiBlueprint::getProjectId).eq(projectId))
                .onSuccess(list -> ok(context, list.isEmpty() ? null : list.get(0)))
                .onFailure(context::fail);
    }

    @Override
    public void upsertBlueprint(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        JsonObject payload = body(context);
        blueprintMapper.list(field(ProjectAiBlueprint::getProjectId).eq(projectId)).compose(list -> {
            LocalDateTime now = LocalDateTime.now();
            if (list.isEmpty()) {
                ProjectAiBlueprint blueprint = new ProjectAiBlueprint();
                blueprint.setId(UUID.randomUUID());
                blueprint.setProjectId(UUID.fromString(projectId));
                blueprint.setDescription(payload.getString("description"));
                blueprint.setConventions(payload.getString("conventions"));
                blueprint.setMemory(payload.getJsonObject("memory", new JsonObject()));
                blueprint.setCreateTime(now);
                blueprint.setUpdateTime(now);
                return blueprintMapper.save(blueprint).map(ok -> blueprint);
            }
            ProjectAiBlueprint blueprint = list.get(0);
            if (payload.containsKey("description")) {
                blueprint.setDescription(payload.getString("description"));
            }
            if (payload.containsKey("conventions")) {
                blueprint.setConventions(payload.getString("conventions"));
            }
            if (payload.containsKey("memory")) {
                blueprint.setMemory(payload.getJsonObject("memory"));
            }
            blueprint.setUpdateTime(now);
            return blueprintMapper.update(blueprint).map(ok -> blueprint);
        }).onSuccess(blueprint -> ok(context, blueprint)).onFailure(context::fail);
    }

    // ── L2 会话 ───────────────────────────────────────────────────────────

    @Override
    public void createSession(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        JsonObject payload = body(context);
        LocalDateTime now = LocalDateTime.now();
        ProjectAiSession session = new ProjectAiSession();
        session.setId(UUID.randomUUID());
        session.setProjectId(UUID.fromString(projectId));
        session.setTitle(payload.getString("title"));
        session.setStatus(payload.getString("status"));
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.save(session)
                .onSuccess(ok -> ok(context, session))
                .onFailure(context::fail);
    }

    @Override
    public void listSessions(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        sessionMapper.list(field(ProjectAiSession::getProjectId).eq(projectId)).onSuccess(list -> {
            list.sort(Comparator.comparing(ProjectAiSession::getUpdateTime,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            ok(context, list);
        }).onFailure(context::fail);
    }

    @Override
    public void getSession(RoutingContext context) {
        sessionMapper.getById(context.pathParam("sessionId"))
                .onSuccess(session -> ok(context, session))
                .onFailure(context::fail);
    }

    @Override
    public void updateSession(RoutingContext context) {
        String sessionId = context.pathParam("sessionId");
        JsonObject payload = body(context);
        sessionMapper.getById(sessionId).compose(session -> {
            if (session == null) {
                return Future.failedFuture(new ApiException(500, "不存在的会话ID"));
            }
            if (payload.containsKey("title")) session.setTitle(payload.getString("title"));
            if (payload.containsKey("status")) session.setStatus(payload.getString("status"));
            if (payload.containsKey("summary")) session.setSummary(payload.getString("summary"));
            if (payload.containsKey("facts")) session.setFacts(payload.getJsonObject("facts"));
            if (payload.containsKey("windowFromSeq")) session.setWindowFromSeq(payload.getLong("windowFromSeq"));
            if (payload.containsKey("timeline")) session.setTimeline(payload.getJsonArray("timeline"));
            session.setUpdateTime(LocalDateTime.now());
            return sessionMapper.update(session).map(ok -> session);
        }).onSuccess(session -> ok(context, session)).onFailure(context::fail);
    }

    @Override
    public void deleteSession(RoutingContext context) {
        String sessionId = context.pathParam("sessionId");
        // 级联删除：任务 + 会话/任务下的全部消息（append-only 无软删，直接物理清理）
        taskMapper.list(field(ProjectAiTask::getSessionId).eq(sessionId)).compose(tasks -> {
            List<Object> ownerIds = new ArrayList<>();
            ownerIds.add(sessionId);
            ownerIds.addAll(tasks.stream().map(t -> t.getId().toString()).collect(Collectors.toList()));
            Condition msgCondition = field(ProjectAiMessage::getOwnerId).in(ownerIds);
            return messageMapper.delete(msgCondition, Map.of())
                    .compose(ok -> taskMapper.delete(field(ProjectAiTask::getSessionId).eq(sessionId), Map.of()))
                    .compose(ok -> sessionMapper.deleteById(sessionId));
        }).onSuccess(ok -> ok(context, Boolean.TRUE)).onFailure(context::fail);
    }

    // ── L3 任务台账 ───────────────────────────────────────────────────────

    @Override
    public void createTask(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        String sessionId = context.pathParam("sessionId");
        JsonObject payload = body(context);
        LocalDateTime now = LocalDateTime.now();
        ProjectAiTask task = new ProjectAiTask();
        task.setId(UUID.randomUUID());
        task.setSessionId(UUID.fromString(sessionId));
        task.setProjectId(UUID.fromString(projectId));
        if (StringUtils.isNotEmpty(payload.getString("processorId"))) {
            task.setProcessorId(UUID.fromString(payload.getString("processorId")));
        }
        task.setRequirement(payload.getString("requirement"));
        task.setStatus(payload.getString("status"));
        task.setCreateTime(now);
        task.setUpdateTime(now);
        taskMapper.save(task)
                .onSuccess(ok -> ok(context, task))
                .onFailure(context::fail);
    }

    @Override
    public void listTasks(RoutingContext context) {
        String sessionId = context.pathParam("sessionId");
        taskMapper.list(field(ProjectAiTask::getSessionId).eq(sessionId)).onSuccess(list -> {
            list.sort(Comparator.comparing(ProjectAiTask::getCreateTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            ok(context, list);
        }).onFailure(context::fail);
    }

    @Override
    public void getTask(RoutingContext context) {
        taskMapper.getById(context.pathParam("taskId"))
                .onSuccess(task -> ok(context, task))
                .onFailure(context::fail);
    }

    @Override
    public void updateTask(RoutingContext context) {
        String taskId = context.pathParam("taskId");
        JsonObject payload = body(context);
        taskMapper.getById(taskId).compose(task -> {
            if (task == null) {
                return Future.failedFuture(new ApiException(500, "不存在的任务ID"));
            }
            if (payload.containsKey("processorId")) {
                String processorId = payload.getString("processorId");
                task.setProcessorId(StringUtils.isEmpty(processorId) ? null : UUID.fromString(processorId));
            }
            if (payload.containsKey("requirement")) task.setRequirement(payload.getString("requirement"));
            if (payload.containsKey("status")) task.setStatus(payload.getString("status"));
            if (payload.containsKey("summary")) task.setSummary(payload.getString("summary"));
            if (payload.containsKey("facts")) task.setFacts(payload.getJsonObject("facts"));
            if (payload.containsKey("windowFromSeq")) task.setWindowFromSeq(payload.getLong("windowFromSeq"));
            if (payload.containsKey("workflow")) task.setWorkflow(payload.getJsonObject("workflow"));
            if (payload.containsKey("result")) task.setResult(payload.getJsonObject("result"));
            if (payload.containsKey("timeline")) task.setTimeline(payload.getJsonArray("timeline"));
            task.setUpdateTime(LocalDateTime.now());
            return taskMapper.update(task).map(ok -> task);
        }).onSuccess(task -> ok(context, task)).onFailure(context::fail);
    }

    // ── 统一消息流 ───────────────────────────────────────────────────────

    @Override
    public void appendMessage(RoutingContext context) {
        JsonObject payload = body(context);
        String ownerType = payload.getString("ownerType");
        String ownerId = payload.getString("ownerId");
        if (StringUtils.isEmpty(ownerType) || StringUtils.isEmpty(ownerId)) {
            context.fail(new ApiException(400, "ownerType / ownerId 不能为空"));
            return;
        }
        JsonObject messagePayload = payload.getJsonObject("payload");
        Integer tokenCount = payload.getInteger("tokenCount");
        // seq 由服务端权威分配（max+1），唯一索引 (owner_type,owner_id,seq) 兜住并发重号
        messageMapper.nextSeq(ownerType, ownerId).compose(seq -> {
            ProjectAiMessage message = new ProjectAiMessage();
            message.setId(UUID.randomUUID());
            message.setOwnerType(ownerType);
            message.setOwnerId(UUID.fromString(ownerId));
            message.setSeq(seq);
            message.setPayload(messagePayload);
            message.setTokenCount(tokenCount);
            message.setCompacted(Boolean.FALSE);
            message.setCreateTime(LocalDateTime.now());
            return messageMapper.save(message).map(ok -> message);
        }).onSuccess(message -> ok(context, JsonObject.of("id", message.getId().toString(), "seq", message.getSeq())))
                .onFailure(context::fail);
    }

    @Override
    public void listMessages(RoutingContext context) {
        String ownerType = context.request().getParam("ownerType");
        String ownerId = context.request().getParam("ownerId");
        if (StringUtils.isEmpty(ownerType) || StringUtils.isEmpty(ownerId)) {
            context.fail(new ApiException(400, "ownerType / ownerId 不能为空"));
            return;
        }
        String fromSeqParam = context.request().getParam("fromSeq");
        long fromSeq = StringUtils.isEmpty(fromSeqParam) ? 0L : Long.parseLong(fromSeqParam);
        Condition condition = field(ProjectAiMessage::getOwnerType).eq(ownerType)
                .and(field(ProjectAiMessage::getOwnerId).eq(ownerId))
                .and(field(ProjectAiMessage::getSeq).ge(fromSeq));
        messageMapper.list(condition).onSuccess(list -> {
            list.sort(Comparator.comparing(ProjectAiMessage::getSeq,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            ok(context, list);
        }).onFailure(context::fail);
    }
}
