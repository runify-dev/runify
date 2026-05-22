package com.run.common.util;

import com.run.common.constants.MessageConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.queue.MessageQueue;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.mapper.ConversationMessageMapper;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeSerialize;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 公共工作流执行器：SSE 响应 + 流式推送 + 持久化 + 异常处理。
 * <p>
 * 两种入口：
 * <ul>
 *   <li>{@link #executeWithQuestion} — 带 question，内部自动计算节点上下文</li>
 *   <li>{@link #execute}             — 简单版，调用方传入所有参数</li>
 * </ul>
 */
public class ConversationWorkflowExecutor {

    private final MessageQueue messageQueue;
    private final ConversationMessageMapper conversationMessageMapper;

    public ConversationWorkflowExecutor(MessageQueue messageQueue,
                                        ConversationMessageMapper conversationMessageMapper) {
        this.messageQueue = messageQueue;
        this.conversationMessageMapper = conversationMessageMapper;
    }


    /**
     * 带 question 的入口，内部自动推断起始节点和节点上下文。
     */
    public void executeWithQuestion(RoutingContext context,
                                    JsonObject workflow,
                                    UUID conversationId,
                                    UUID applicationId,
                                    UUID workflowRunId,
                                    List<ConversationMessage> conversationMessages,
                                    Content question) {

        List<ConversationMessage> sorted = sortByTime(conversationMessages);

        Optional<ConversationMessage> first = conversationMessages.stream()
                .filter(c -> Strings.CS.equals(c.getWorkflowRunId().toString(), workflowRunId.toString()))
                .filter(c -> c.getType().equals(MessageConstants.ASSISTANT))
                .max(Comparator.comparing(ConversationMessage::getCreateTime));

        DefaultKeyValue<Function<WorkFlow, Node>, Map<String, Map<String, Object>>> kv =
                resolveNodeContext(question, sorted.size() > 2
                        ? first.map(ConversationMessage::getContext).orElse(new JsonArray())
                        : new JsonArray());

        execute(context, workflow, conversationId, applicationId,
                workflowRunId, conversationMessages,
                Map.of("content", JsonObject.mapFrom(question),
                        "workflowRunId", workflowRunId.toString()),
                kv.getValue(),
                kv.getKey());
    }

    // ==================== 入口2：通用 ====================

    /**
     * 通用入口，调用方自己准备好所有差异化参数。
     *
     * @param context              Vert.x 路由上下文
     * @param workflow             工作流定义 JSON
     * @param conversationId       会话 ID
     * @param applicationId        应用 ID
     * @param workflowRunId        本次运行 ID
     * @param conversationMessages 历史消息（内部会按时间排序）
     * @param extraParams          额外参数，合并到基础 params（如 content、workflowRunId）
     * @param nodeConfig           节点配置，无则传 new HashMap&lt;&gt;()
     * @param startNodeSelector    起始节点选择器，无则传 null
     */
    public void execute(RoutingContext context,
                        JsonObject workflow,
                        UUID conversationId,
                        UUID applicationId,
                        UUID workflowRunId,
                        List<ConversationMessage> conversationMessages,
                        Map<String, Object> extraParams,
                        Map<String, Map<String, Object>> nodeConfig,
                        Function<WorkFlow, Node> startNodeSelector
    ) {
        try {
            List<ConversationMessage> sorted = sortByTime(conversationMessages);
            initSseResponse(context);
            messageQueue.create(conversationId.toString());
            ensureToolStateDir(conversationId);

            Map<String, Object> params = new HashMap<>(Map.of(
                    "messages", sorted,
                    "conversationId", conversationId,
                    "applicationId", applicationId));
            params.putAll(extraParams);

            AtomicLong index = new AtomicLong(1);

            WorkFlowManage workFlowManage = new WorkFlowManage(
                    WorkFlow.of(workflow, WorkflowType.CHAT_WORKFLOW),
                    params,
                    nodeConfig,
                    (wm, node, chunk, isEnd) -> {
                        if (isEnd) {
                            onComplete(context, wm, conversationId, applicationId, workflowRunId);
                        } else {
                            onChunk(context, conversationId, index, chunk);
                        }
                    },
                    startNodeSelector);

            WorkflowRunRegistry.register(conversationId.toString(), workFlowManage);
            workFlowManage.invoke();

        } catch (Exception e) {
            handleError(context, conversationId, workflowRunId, e);
        }
    }

    // ==================== 节点上下文推断（原 get 方法） ====================

    private DefaultKeyValue<Function<WorkFlow, Node>, Map<String, Map<String, Object>>>
    resolveNodeContext(Content content, JsonArray context) {
        if (content.getPosition() != null) {
            HashMap<String, Map<String, Object>> nodeContext = new HashMap<>();
            for (int i = 0; i < context.size(); i++) {
                NodeSerialize nodeSerialize = context.getJsonObject(i).mapTo(NodeSerialize.class);
                nodeContext.put(nodeSerialize.getNodeInfo().getId(), nodeSerialize.getContext().getMap());
            }
            return new DefaultKeyValue<>(wm -> wm.getNode(content.getPosition().id()), nodeContext);
        } else {
            return new DefaultKeyValue<>(wm -> wm.getNode("start-node"), new HashMap<>());
        }
    }


    /**
     * SSE 初始化
     *
     * @param context 上下文
     */
    private void initSseResponse(RoutingContext context) {
        context.response()
                .setChunked(true)
                .putHeader("Content-Type", "text/event-stream;charset=utf-8")
                .putHeader("Cache-Control", "no-cache")
                .putHeader("Character-Encoding", "utf-8");
        context.response().write(Buffer.buffer("", "utf-8"));
    }

    /**
     * 工具状态目录
     *
     * @param conversationId 对话id
     * @throws Exception 错误
     */
    private void ensureToolStateDir(UUID conversationId) throws Exception {
        Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId, "_tool_state");
        Files.createDirectories(basePath);
    }


    /**
     * 排序
     *
     * @param messages messages
     * @return 排序后的
     */
    private List<ConversationMessage> sortByTime(List<ConversationMessage> messages) {
        return messages.stream()
                .sorted(Comparator.comparing(ConversationMessage::getCreateTime))
                .toList();
    }


    /**
     * 流式回调： chunk
     *
     * @param context        上下文
     * @param conversationId 对话id
     * @param index          chunk下标
     * @param chunk          chunk 块
     */
    private void onChunk(RoutingContext context, UUID conversationId,
                         AtomicLong index, Content chunk) {
        Message message = new Message(List.of(chunk), index.getAndIncrement());
        String data = "data: " + JacksonUtils.toJson(message) + "\n\n";
        messageQueue.publish(conversationId.toString(), message.index(), data);
        context.response().write(Buffer.buffer(data, "utf-8"));
    }


    /**
     * 流式回调：完成
     *
     * @param context        上下文
     * @param wm             工作流管理器
     * @param conversationId 对话id
     * @param applicationId  应用id
     * @param workflowRunId  工作流运行id
     */
    private void onComplete(RoutingContext context, WorkFlowManage wm,
                            UUID conversationId, UUID applicationId, UUID workflowRunId) {
        WorkflowRunRegistry.unregister(conversationId.toString());
        messageQueue.complete(conversationId.toString());
        messageQueue.delete(conversationId.toString());

        ConversationMessage msg = new ConversationMessage(
                UUID.randomUUID(),
                conversationId,
                applicationId,
                workflowRunId,
                MessageConstants.ASSISTANT,
                new JsonArray(wm.getChunks()),
                new JsonArray(wm.getNodes().stream().map(INode::serialize).toList()),
                wm.getPromptTokens(),
                wm.getCompletionTokens(),
                wm.getRuntime(),
                LocalDateTime.now(),
                LocalDateTime.now());

        conversationMessageMapper.batch_save(List.of(msg))
                .onSuccess(_ -> {
                    wm.clear();
                    context.response().end();
                })
                .onFailure(context::fail);
    }


    /**
     * 异常处理
     *
     * @param context        上下文
     * @param conversationId 对户aid
     * @param workflowRunId  工作流运行ID
     * @param e              异常
     */
    private void handleError(RoutingContext context, UUID conversationId,
                             UUID workflowRunId, Exception e) {
        Message message = new Message(
                List.of(new FailureContent(
                        e.getMessage(),
                        workflowRunId.toString(),
                        CommonUtils.uuid7().toString(),
                        NodeStatus.FAIL, "", "")),
                0);
        String data = "data: " + JacksonUtils.toJson(message) + "\n\n";
        context.end(Buffer.buffer(data, "utf-8"));
        WorkflowRunRegistry.unregister(conversationId.toString());
        messageQueue.complete(conversationId.toString());
    }
}