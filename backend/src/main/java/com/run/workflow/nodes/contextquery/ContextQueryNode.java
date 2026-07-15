package com.run.workflow.nodes.contextquery;

import com.run.RunApplication;
import com.run.common.constants.MessageConstants;
import com.run.dao.entity.ConversationMessage;
import com.run.dao.entity.CtxFact;
import com.run.dao.entity.CtxSummary;
import com.run.dao.mapper.ConversationMessageMapper;
import com.run.dao.mapper.CtxFactMapper;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.run.sql.condition.Condition;

import static com.run.sql.DSL.field;

/**
 * 上下文查询节点（载入阶段）：对话开始时在循环外查一次，为 context-manage 产出各"初始值"。
 * <p>
 * 载入 = 摘要 + 游标后回合（原规格 §9）：
 * <ol>
 *     <li>ctx_summary 一行（scope=conversation）→ 摘要种子 + covered_upto 游标</li>
 *     <li>conversation_message：create_time &gt; covered_upto 的回合（排除本次 run 的行——
 *     其内容会由 push 进入变化值，避免重复），拍平 content → 历史上下文种子</li>
 *     <li>ctx_fact：merge(应用级非观察期, 对话级)，对话级覆盖同键 → 便签种子（含产物 artifact 子区）</li>
 * </ol>
 * 输出：history / summary / facts / historyUpto（载入边界，context-save-node 推进游标用）。
 * 查询量与对话总长无关（游标 + 上限）。任何失败输出全空放行，不阻断对话。
 */
@Slf4j
public class ContextQueryNode extends INode<ContextQueryNode, ContextQueryNode.NodeData> {
    public final static String type = "context-query-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final long DB_TIMEOUT_SECONDS = 10;
    /**
     * 游标后回合的查询上限：与对话总长无关，查询量恒定
     */
    private static final int MAX_ROUNDS = 200;

    /**
     * 无配置项——会话标识从运行参数取；预留空配置类以符合节点范式
     */
    public static class NodeData {
    }

    public ContextQueryNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ContextQueryNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ContextQueryNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ContextQueryNode node) {
            List<Map<String, Object>> history = List.of();
            List<Map<String, Object>> full = List.of();
            String summary = "";
            List<Map<String, Object>> facts = List.of();
            String historyUpto = "";
            try {
                String conversationId = Objects.toString(workFlowManage.getParams().get("conversationId"), null);
                String applicationId = Objects.toString(workFlowManage.getParams().get("applicationId"), null);
                String workflowRunId = Objects.toString(workFlowManage.getParams().get("workflowRunId"), null);

                if (StringUtils.isNotBlank(conversationId)) {
                    CtxSummary summaryRow = await(RunApplication.appComponent.ctxSummaryMapper().one(
                            field(CtxSummary::getScopeType).eq("conversation")
                                    .and(field(CtxSummary::getScopeId).eq(conversationId)), Map.of()));
                    LocalDateTime coveredUpto = null;
                    if (summaryRow != null) {
                        summary = StringUtils.defaultString(summaryRow.getSummaryText());
                        coveredUpto = summaryRow.getCoveredUpto();
                    }

                    HistoryResult historyResult = loadHistory(conversationId, coveredUpto, workflowRunId);
                    history = historyResult.contents();
                    full = historyResult.full();
                    historyUpto = historyResult.upto();

                    facts = loadFacts(conversationId, applicationId,
                            userScopeId(workFlowManage, applicationId));
                }
            } catch (Exception e) {
                // 载入失败 → 空初始值放行（对话从零开始），不阻断
                log.warn("context-query 载入失败，输出空初始值", e);
            }
            workFlowManage.writeContext(node, "history", history);
            // full = history + 本轮当前问题（不排除本轮）：喂给"需含当前发言"的消费者（如便签提取）。
            // 注意：勿把 full 接给 context-manage 种子——当前回合会与实时 push 重复计一遍。
            workFlowManage.writeContext(node, "full", full);
            workFlowManage.writeContext(node, "summary", summary);
            workFlowManage.writeContext(node, "facts", facts);
            workFlowManage.writeContext(node, "historyUpto", historyUpto);
            node.status = NodeStatus.SUCCESS;
            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private record HistoryResult(List<Map<String, Object>> contents, List<Map<String, Object>> full, String upto) {
        }

        /**
         * 游标后回合：create_time > covered_upto，排除本次 run 的行，拍平 content 为 Content 列表
         */
        private HistoryResult loadHistory(String conversationId, LocalDateTime coveredUpto, String workflowRunId) {
            ConversationMessageMapper mapper = RunApplication.appComponent.conversationMessageMapper();
            List<ConversationMessage> rows = await(mapper.list(mapper.select()
                    .where(field(ConversationMessage::getConversationId).eq(conversationId))
                    .orderBy(field(ConversationMessage::getCreateTime).desc())
                    .limit(MAX_ROUNDS).render()));
            Collections.reverse(rows);

            List<Map<String, Object>> contents = new ArrayList<>();
            List<Map<String, Object>> full = new ArrayList<>();
            LocalDateTime upto = null;
            for (ConversationMessage row : rows) {
                if (row.getType() != MessageConstants.USER && row.getType() != MessageConstants.ASSISTANT) {
                    continue;
                }
                if (coveredUpto != null && row.getCreateTime() != null
                        && !row.getCreateTime().isAfter(coveredUpto)) {
                    continue;
                }
                // 本轮的行：history 排除（避免与实时 push 重复），full 保留（= 含当前问题）
                boolean currentRun = workflowRunId != null && row.getWorkflowRunId() != null
                        && workflowRunId.equals(row.getWorkflowRunId().toString());
                JsonArray content = row.getContent();
                if (content != null) {
                    // 给每条 Content 打上所属回合的创建时间：context-manage 据此算出覆盖游标时间戳，
                    // context-save 再据此推进 covered_upto（部分覆盖也能推进，行边界安全）
                    String createTime = row.getCreateTime() != null ? row.getCreateTime().toString() : null;
                    for (Object o : content) {
                        JsonObject jo = o instanceof JsonObject j ? j
                                : (o instanceof Map<?, ?> map ? new JsonObject((Map<String, Object>) map) : null);
                        if (jo == null) {
                            continue;
                        }
                        if (createTime != null) {
                            jo.put("createTime", createTime);
                        }
                        Map<String, Object> map = jo.getMap();
                        full.add(map);
                        if (!currentRun) {
                            contents.add(map);
                        }
                    }
                }
                // upto（游标）只跟已固化历史走，不含当前——否则会把还没被摘的当前回合游标推过头
                if (!currentRun && row.getCreateTime() != null && (upto == null || row.getCreateTime().isAfter(upto))) {
                    upto = row.getCreateTime();
                }
            }
            return new HistoryResult(contents, full, upto == null ? "" : upto.toString());
        }

        /**
         * 便签种子：merge(应用级非观察期 → 用户级非观察期 → 对话级)，后者覆盖同键。
         * 优先级 = 越具体越优先：application(全 app) &lt; user(该用户) &lt; conversation(本次)。
         */
        private List<Map<String, Object>> loadFacts(String conversationId, String applicationId, String userScopeId) {
            CtxFactMapper mapper = RunApplication.appComponent.ctxFactMapper();
            Map<String, CtxFact> merged = new LinkedHashMap<>();
            if (StringUtils.isNotBlank(applicationId)) {
                mergeNonProvisional(merged, mapper, "application", applicationId, null);
            }
            if (StringUtils.isNotBlank(userScopeId)) {
                mergeNonProvisional(merged, mapper, "user", userScopeId, applicationId);
            }
            for (CtxFact fact : await(mapper.list(
                    field(CtxFact::getScopeType).eq("conversation")
                            .and(field(CtxFact::getScopeId).eq(conversationId)), Map.of()))) {
                merged.put(fact.getSubtype() + "|" + fact.getFactKey(), fact);
            }
            return merged.values().stream()
                    .map(f -> (Map<String, Object>) new LinkedHashMap<String, Object>(Map.of(
                            "subtype", f.getSubtype(), "key", f.getFactKey(), "value", f.getFactValue())))
                    .toList();
        }

        /**
         * 载入某 scope 的非观察期（provisional != true）便签，同键覆盖进 merged。
         * applicationId 非空时追加 per-app 隔离条件（仅 user 档需要，scope_id=userId 跨应用不唯一）。
         */
        private void mergeNonProvisional(Map<String, CtxFact> merged, CtxFactMapper mapper,
                                         String scopeType, String scopeId, String applicationId) {
            Condition where = field(CtxFact::getScopeType).eq(scopeType)
                    .and(field(CtxFact::getScopeId).eq(scopeId));
            if (applicationId != null) {
                where = where.and(field(CtxFact::getApplicationId).eq(applicationId));
            }
            for (CtxFact fact : await(mapper.list(where, Map.of()))) {
                if (!Boolean.TRUE.equals(fact.getProvisional())) {
                    merged.put(fact.getSubtype() + "|" + fact.getFactKey(), fact);
                }
            }
        }

        /**
         * user 档 scope_id = 裸 userId；per-app 隔离由 loadFacts 追加的 application_id 条件承担。
         * 仅登录用户成立，匿名返回 null。与 context-save-node 的组装规则严格一致，保证写入/载入命中同一 scope。
         */
        private String userScopeId(WorkFlowManage workFlowManage, String applicationId) {
            if (StringUtils.isBlank(applicationId)) {
                return null;
            }
            Object raw = workFlowManage.getParams().get("user");
            JsonObject user = raw instanceof JsonObject jo ? jo
                    : (raw instanceof Map<?, ?> map ? new JsonObject((Map<String, Object>) map) : null);
            if (user == null || !"USER".equals(user.getString("type"))) {
                return null;
            }
            String userId = user.getString("id");
            return StringUtils.isBlank(userId) ? null : userId;
        }

        private static <T> T await(Future<T> future) {
            try {
                return future.toCompletionStage().toCompletableFuture()
                        .get(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public NodeData getNodeData(JsonObject params) {
        return new NodeData();
    }

    @Override
    public NodeResult<ContextQueryNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
