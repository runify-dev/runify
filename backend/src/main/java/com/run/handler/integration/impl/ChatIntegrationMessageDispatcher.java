package com.run.handler.integration.impl;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.constants.ConversationExecuteConstants;
import com.run.common.constants.MessageConstants;
import com.run.auth.constants.TokenTypeConstants;
import com.run.dao.entity.Conversation;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.entity.Integration;
import com.run.dao.mapper.ApplicationMapper;
import com.run.dao.mapper.ConversationMapper;
import com.run.dao.mapper.ConversationMessageMapper;
import com.run.handler.integration.IIntegrationMessageDispatcher;
import com.run.workflow.INode;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.ContentConverter;
import com.run.workflow.message.struct.TextContent;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 入站消息分发器: 为 IM 发送者维护会话, 无 HTTP 跑通工作流并聚合完整回复文本 }
 */
public class ChatIntegrationMessageDispatcher implements IIntegrationMessageDispatcher {

    private final ApplicationMapper applicationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;

    @Inject
    public ChatIntegrationMessageDispatcher(ApplicationMapper applicationMapper,
                                            ConversationMapper conversationMapper,
                                            ConversationMessageMapper conversationMessageMapper) {
        this.applicationMapper = applicationMapper;
        this.conversationMapper = conversationMapper;
        this.conversationMessageMapper = conversationMessageMapper;
    }

    @Override
    public Future<String> dispatch(Integration integration, String fromUser, JsonObject content) {
        return run(integration, fromUser, content, null, null);
    }

    @Override
    public Future<String> dispatchStream(Integration integration, String fromUser, JsonObject content, Consumer<String> onDelta) {
        return run(integration, fromUser, content, onDelta, null);
    }

    @Override
    public Future<String> dispatchSegments(Integration integration, String fromUser, JsonObject content,
                                           java.util.function.BiConsumer<String, String> onSegment) {
        return run(integration, fromUser, content, null, onSegment);
    }

    private Future<String> run(Integration integration, String fromUser, JsonObject content,
                               Consumer<String> onDelta, java.util.function.BiConsumer<String, String> onSegment) {
        UUID applicationId = integration.getApplicationId();
        if (applicationId == null) {
            return Future.failedFuture("integration not bound to an application");
        }
        String userKey = integration.getType() + ":" + fromUser;
        return findOrCreateConversation(applicationId, userKey, fromUser).compose(conversation -> {
            UUID conversationId = conversation.getId();
            UUID workflowRunId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            // 与对话接口一致: ContentConverter.of(content, workflowRunId)
            Content question = ContentConverter.of(content, workflowRunId.toString());

            ConversationMessage userMessage = new ConversationMessage(UUID.randomUUID(),
                    conversationId, applicationId, workflowRunId,
                    MessageConstants.USER,
                    new JsonArray(List.of(question)),
                    new JsonArray(),
                    0, 0, 0L, now, now);

            return conversationMessageMapper.save(userMessage)
                    .compose(ok -> loadHistory(conversationId))
                    .compose(history -> applicationMapper.getById(applicationId.toString())
                            .compose(app -> runWorkflow(app.getWorkflow(), conversationId, applicationId, workflowRunId, history, question, onDelta, onSegment)));
        });
    }

    private Future<Conversation> findOrCreateConversation(UUID applicationId, String userKey, String name) {
        return conversationMapper.list(field(Conversation::getApplicationId).eq(applicationId.toString())
                        .and(field(Conversation::getConversationUserId).eq(userKey)))
                .compose(list -> {
                    if (list != null && !list.isEmpty()) {
                        return Future.succeededFuture(list.getFirst());
                    }
                    LocalDateTime now = LocalDateTime.now();
                    Conversation conversation = new Conversation(UUID.randomUUID(),
                            applicationId, name, ConversationExecuteConstants.CONVERSATION,
                            new JsonObject(), userKey, TokenTypeConstants.USER,
                            0, 0, 0, 0, Boolean.FALSE, now, now);
                    return conversationMapper.save(conversation).map(ok -> conversation);
                });
    }

    private Future<List<ConversationMessage>> loadHistory(UUID conversationId) {
        return conversationMessageMapper.list(conversationMessageMapper.select()
                .where(field(ConversationMessage::getConversationId).eq(conversationId.toString()))
                .orderBy(field(ConversationMessage::getCreateTime).desc())
                .limit(10).render());
    }

    private Future<String> runWorkflow(JsonObject workflow,
                                       UUID conversationId,
                                       UUID applicationId,
                                       UUID workflowRunId,
                                       List<ConversationMessage> history,
                                       Content question,
                                       Consumer<String> onDelta,
                                       java.util.function.BiConsumer<String, String> onSegment) {
        Promise<String> promise = Promise.promise();
        try {
            ensureToolStateDir(applicationId, conversationId);
            List<ConversationMessage> sorted = history.stream()
                    .sorted(Comparator.comparing(ConversationMessage::getCreateTime))
                    .toList();

            Map<String, Object> params = new HashMap<>();
            params.put("messages", sorted);
            params.put("conversationId", conversationId);
            params.put("applicationId", applicationId);
            params.put("content", JsonObject.mapFrom(question));
            params.put("workflowRunId", workflowRunId.toString());

            final long[] lastEmit = {0L};
            final java.util.Set<String> flushed = java.util.concurrent.ConcurrentHashMap.newKeySet();
            WorkFlowManage workFlowManage = new WorkFlowManage(
                    WorkFlow.of(workflow, WorkflowType.CHAT_WORKFLOW),
                    params,
                    new HashMap<>(),
                    (wm, node, chunk, isEnd) -> {
                        if (isEnd) {
                            // 结束: 分段模式按顺序补发所有未发的块; 快照模式补一帧完整快照
                            if (onSegment != null) {
                                List<Content> all = wm.getChunks();
                                System.out.println("[seg] isEnd chunks=" + all.size());
                                for (Content c : all) {
                                    JsonObject o = JsonObject.mapFrom(c);
                                    Object cont = o.getValue("content");
                                    int len = cont instanceof String s ? s.length() : -1;
                                    String key = segKey(c);
                                    boolean already = key != null && flushed.contains(key);
                                    System.out.println("[seg]   chunk id=" + c.getId() + " type=" + o.getString("type")
                                            + " status=" + o.getString("status") + " len=" + len + " alreadyFlushed=" + already);
                                }
                                for (Content c : all) {
                                    String key = segKey(c);
                                    if (key != null && flushed.add(key)) {
                                        System.out.println("[seg] end-flush key=" + key);
                                        flushSegment(wm, c.getId(), c.getType(), onSegment);
                                    }
                                }
                            } else {
                                emitSnapshot(onDelta, wm);
                            }
                            onComplete(wm, conversationId, applicationId, workflowRunId, promise);
                        } else if (onSegment != null && chunk != null) {
                            // 分段: chunk 的 status 进入终态(非 RUNNING/BEFORE_RUNNING)说明该块完成 -> 立即发, 每 (id,type) 一次。
                            // 注意: reasoning 与 text 复用同一 chunkId(见 AIChat), 去重必须带上 type, 否则 reasoning 结束
                            // 占用该 id 后, 同 id 的正文 text 会被误判已发而整段丢失。
                            String st = JsonObject.mapFrom(chunk).getString("status", "");
                            if (!st.isEmpty() && !"RUNNING".equals(st) && !"BEFORE_RUNNING".equals(st)) {
                                String key = segKey(chunk);
                                if (key != null && flushed.add(key)) {
                                    System.out.println("[seg] stream-flush key=" + key + " status=" + st);
                                    flushSegment(wm, chunk.getId(), chunk.getType(), onSegment);
                                }
                            }
                        } else if (onDelta != null) {
                            // 快照节流: renderFull 遍历所有 chunk(mapFrom), 每 token 跑是 O(N^2); 最多每 800ms 一次
                            long now = System.currentTimeMillis();
                            if (now - lastEmit[0] >= 800) {
                                lastEmit[0] = now;
                                emitSnapshot(onDelta, wm);
                            }
                        }
                    },
                    w -> w.getNode("start-node"));

            workFlowManage.invoke();
        } catch (Exception e) {
            promise.fail(e);
        }
        return promise.future();
    }

    /**
     * 渲染当前快照并回调; 吞掉异常(跑在节点 write 路径, 抛出会中断工作流)
     */
    /**
     * 聚合层按 id+type 区分内容块(见 AggregationManager), 去重键也必须一致, 否则同 id 的 reasoning/text 互相覆盖
     */
    private static String segKey(Content c) {
        if (c == null || c.getId() == null) {
            return null;
        }
        return c.getId() + "_" + (c.getType() == null ? "" : c.getType().name());
    }

    /**
     * 把某个 (id,type) 的内容块(已聚合完整)按 (type, content) 回调出去
     */
    private void flushSegment(WorkFlowManage wm, String id, ContentTypeConstants type,
                              java.util.function.BiConsumer<String, String> onSegment) {
        try {
            for (Content c : wm.getChunks()) {
                if (id.equals(c.getId()) && type == c.getType()) {
                    JsonObject o = JsonObject.mapFrom(c);
                    String typeName = type == null ? "" : type.name();
                    if ("TEXT".equals(typeName)) {
                        if (o.getValue("content") instanceof String s && !s.isEmpty()) {
                            System.out.println("[seg] emit TEXT id=" + id + " len=" + s.length());
                            onSegment.accept("TEXT", s);
                        } else {
                            System.out.println("[seg] emit TEXT id=" + id + " SKIP(empty/non-string) val=" + o.getValue("content"));
                        }
                    } else if ("REASONING".equals(typeName)) {
                        if (o.getValue("content") instanceof String s && !s.isEmpty()) {
                            onSegment.accept("REASONING", "💭 " + s);
                        }
                    } else if ("TOOL".equals(typeName)) {
                        onSegment.accept("TOOL", "🔧 调用工具 `" + o.getString("toolName", "tool") + "`");
                    } else if ("FAILURE".equals(typeName)) {
                        // 工作流失败块: 之前未处理 -> 末段静默丢失, 用户什么都收不到。作为文本发出去。
                        if (o.getValue("content") instanceof String s && !s.isEmpty()) {
                            System.out.println("[seg] emit FAILURE id=" + id + " len=" + s.length());
                            onSegment.accept("TEXT", "⚠️ " + s);
                        }
                    }
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("[integration] flushSegment error: " + e.getMessage());
        }
    }

    private void emitSnapshot(Consumer<String> onDelta, WorkFlowManage wm) {
        if (onDelta == null) {
            return;
        }
        try {
            onDelta.accept(renderFull(wm.getChunks()));
        } catch (Exception e) {
            System.err.println("[integration] onDelta error: " + e.getMessage());
        }
    }

    private void onComplete(WorkFlowManage wm, UUID conversationId, UUID applicationId, UUID workflowRunId, Promise<String> promise) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Content c : wm.getChunks()) {
                JsonObject o = JsonObject.mapFrom(c);
                if ("TEXT".equals(o.getString("type"))) {
                    String piece = o.getString("content");
                    if (piece != null) {
                        sb.append(piece);
                    }
                }
            }
            String text = sb.toString();

            LocalDateTime now = LocalDateTime.now();
            ConversationMessage assistant = new ConversationMessage(UUID.randomUUID(),
                    conversationId, applicationId, workflowRunId,
                    MessageConstants.ASSISTANT,
                    new JsonArray(wm.getChunks()),
                    new JsonArray(wm.getNodes().stream().map(INode::serialize).toList()),
                    wm.getPromptTokens(), wm.getCompletionTokens(), wm.getRuntime(), now, now);

            conversationMessageMapper.batch_save(List.of(assistant)).onComplete(ar -> {
                wm.clear();
                promise.complete(text == null || text.isEmpty() ? "(无回复)" : text);
            });
        } catch (Exception e) {
            promise.fail(e);
        }
    }

    /**
     * 把当前所有内容块渲染成完整 markdown 快照(供流式展示):
     * TEXT 直接拼(含内联图片 markdown); REASONING 用 &lt;think&gt; 包裹; TOOL 显示工具调用状态。其余块跳过。
     */
    private String renderFull(List<Content> chunks) {
        StringBuilder sb = new StringBuilder();
        JsonObject runningTool = null; // 仅展示最新一个未完成的工具, 完成后不再显示
        for (Content c : chunks) {
            JsonObject o = JsonObject.mapFrom(c);
            String type = o.getString("type");
            switch (type == null ? "" : type) {
                case "TEXT" -> {
                    if (o.getValue("content") instanceof String s) {
                        sb.append(s);
                    }
                }
                case "REASONING" -> {
                    if (o.getValue("content") instanceof String s && !s.isEmpty()) {
                        sb.append("<think>").append(s).append("</think>\n");
                    }
                }
                case "TOOL" -> {
                    if (!"SUCCESS".equals(o.getString("status"))) {
                        runningTool = o;
                    }
                }
                default -> {
                }
            }
        }
        if (runningTool != null) {
            sb.append("\n🔧 **").append(runningTool.getString("toolName", "tool")).append("** ⏳ 进行中\n");
            String args = runningTool.getString("functionArguments");
            if (args != null && !args.isBlank()) {
                sb.append("```\n").append(args.strip()).append("\n```\n");
            }
            if (runningTool.getValue("content") instanceof String res && !res.isBlank()) {
                sb.append(res.strip()).append("\n");
            }
        }
        return sb.toString();
    }

    private void ensureToolStateDir(UUID applicationId, UUID conversationId) throws Exception {
        Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + applicationId + "/" + conversationId, "_tool_state");
        Files.createDirectories(basePath);
    }
}
