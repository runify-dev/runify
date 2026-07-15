package com.run.workflow.nodes.contextsave;

import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.CtxFact;
import com.run.dao.entity.CtxSummary;
import com.run.dao.mapper.CtxFactMapper;
import com.run.dao.mapper.CtxSummaryMapper;
import com.run.sql.condition.Condition;
import com.run.workflow.nodes.contextmanage.service.SectionRegistry;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.run.sql.DSL.field;

/**
 * 上下文写入节点（固化阶段）：循环结束后引用摘要 / 便签变化值，落库到跨对话层。
 * <ul>
 *     <li>ctx_summary：摘要文本覆盖 upsert；covered_upto 依摘要对象携带的 coveredUpto 推进
 *     （由 context-manage 算出，覆盖前缀的历史时间戳、部分覆盖也安全）；无 coveredUpto 则游标不动，
 *     下次载入宁可多读原文，避免"摘要没覆盖到的回合再也载不回来"</li>
 *     <li>ctx_fact：便签逐条同键 upsert（含产物 artifact 子区），scope=conversation</li>
 * </ul>
 * 沉淀失败只记日志、节点成功——沉淀是锦上添花，不能毁掉对话。
 */
@Slf4j
public class ContextSaveNode extends INode<ContextSaveNode, ContextSaveNode.NodeData> {
    public final static String type = "context-save-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final long DB_TIMEOUT_SECONDS = 10;

    @Getter
    @Setter
    public static class NodeData {
        /**
         * 摘要引用：{text, covered, seedCovered} 或裸字符串（如 [外循环, summary]）
         */
        private List<String> summaryReference;
        /**
         * 便签引用：元素 {subtype, key, value}（如 [外循环, facts]）
         */
        private List<String> factsReference;
    }

    public ContextSaveNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ContextSaveNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ContextSaveNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ContextSaveNode node) {
            int savedFacts = 0;
            boolean savedSummary = false;
            try {
                String conversationId = Objects.toString(workFlowManage.getParams().get("conversationId"), null);
                if (StringUtils.isNotBlank(conversationId)) {
                    String applicationId = Objects.toString(workFlowManage.getParams().get("applicationId"), null);
                    String userScopeId = userScopeId(workFlowManage, applicationId);
                    // 便签 → scope 归属来自应用级"便签设置"（未配置回退内置默认）
                    Map<String, String> scopeMap = SectionRegistry.scopeMap(applicationId);
                    savedSummary = saveSummary(workFlowManage, node, conversationId);
                    savedFacts = saveFacts(workFlowManage, node, conversationId, applicationId, userScopeId, scopeMap);
                }
            } catch (Exception e) {
                // 沉淀失败只记日志——不能因为记不上便签毁掉对话
                log.warn("context-save 沉淀失败", e);
            }
            workFlowManage.writeContext(node, "savedSummary", savedSummary);
            workFlowManage.writeContext(node, "savedFacts", savedFacts);
            node.status = NodeStatus.SUCCESS;
            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private boolean saveSummary(WorkFlowManage workFlowManage, ContextSaveNode node, String conversationId) {
            Object value = readValue(workFlowManage, node.params.getSummaryReference());
            String text;
            String coveredUpto = null;
            if (value instanceof String s) {
                text = s;
            } else {
                JsonObject json = toJsonObject(value);
                if (json == null) {
                    return false;
                }
                text = json.getString("text", "");
                coveredUpto = json.getString("coveredUpto");
            }
            if (StringUtils.isBlank(text)) {
                return false;
            }

            // 游标推进：用摘要对象携带的 coveredUpto（context-manage 算出的覆盖前缀历史时间戳，
            // 部分覆盖也能推进、行边界安全）；无则游标不动，下次载入多读原文（安全不丢）。
            LocalDateTime newCursor = parseCursor(coveredUpto);

            CtxSummaryMapper mapper = RunApplication.appComponent.ctxSummaryMapper();
            CtxSummary existing = await(mapper.one(
                    field(CtxSummary::getScopeType).eq("conversation")
                            .and(field(CtxSummary::getScopeId).eq(conversationId)), Map.of()));
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                await(mapper.save(new CtxSummary(CommonUtils.uuid7(), "conversation", conversationId,
                        text, newCursor, now, now)));
            } else {
                existing.setSummaryText(text);
                if (newCursor != null && (existing.getCoveredUpto() == null
                        || newCursor.isAfter(existing.getCoveredUpto()))) {
                    existing.setCoveredUpto(newCursor);
                }
                existing.setUpdateTime(now);
                await(mapper.update(existing));
            }
            return true;
        }

        private int saveFacts(WorkFlowManage workFlowManage, ContextSaveNode node,
                              String conversationId, String applicationId, String userScopeId,
                              Map<String, String> scopeMap) {
            Object value = readValue(workFlowManage, node.params.getFactsReference());
            Iterable<?> elements = switch (value) {
                case List<?> list -> list;
                case JsonArray array -> array;
                case null, default -> List.of();
            };
            CtxFactMapper mapper = RunApplication.appComponent.ctxFactMapper();
            LocalDateTime now = LocalDateTime.now();
            int saved = 0;
            for (Object element : elements) {
                JsonObject json = toJsonObject(element);
                if (json == null) {
                    continue;
                }
                String subtype = json.getString("subtype");
                String key = json.getString("key");
                String factValue = json.getString("value");
                if (StringUtils.isBlank(subtype) || StringUtils.isBlank(key) || StringUtils.isBlank(factValue)) {
                    continue;
                }
                // 按便签设置里配置的 scope 分流 owner；未配置的 subtype（如 artifact）兜底 conversation
                Scope scope = resolveScope(scopeMap.getOrDefault(subtype, "conversation"),
                        conversationId, applicationId, userScopeId);
                // application_id 仅 user 档参与定位（scope_id=userId 跨应用不唯一）；
                // conversation/application 档 scope_id 本身已唯一，不加此条件（其行该列为空）
                Condition where = field(CtxFact::getScopeType).eq(scope.type())
                        .and(field(CtxFact::getScopeId).eq(scope.id()))
                        .and(field(CtxFact::getSubtype).eq(subtype))
                        .and(field(CtxFact::getFactKey).eq(key));
                if (scope.applicationId() != null) {
                    where = where.and(field(CtxFact::getApplicationId).eq(scope.applicationId()));
                }
                CtxFact existing = await(mapper.one(where, Map.of()));
                if (existing == null) {
                    await(mapper.save(new CtxFact(CommonUtils.uuid7(), scope.type(), scope.id(),
                            scope.applicationId(), subtype, key, factValue, Boolean.FALSE, now, now)));
                    saved++;
                } else if (!StringUtils.equals(existing.getFactValue(), factValue)) {
                    existing.setFactValue(factValue);
                    existing.setUpdateTime(now);
                    await(mapper.update(existing));
                    saved++;
                }
            }
            return saved;
        }

        /**
         * 便签 owner 归属（scope_type, scope_id, application_id），scope 来自便签设置：
         * <ul>
         *     <li>user → user 档（scope_id=userId + application_id=applicationId 做 per-app 隔离；
         *     匿名/无身份降级 conversation）</li>
         *     <li>application → application 档（无 applicationId 时降级 conversation）</li>
         *     <li>conversation / 其余 → conversation 档</li>
         * </ul>
         * application_id 仅 user 档非空（其余档 scope_id 已全局唯一，无需）。
         */
        private record Scope(String type, String id, String applicationId) {
        }

        private Scope resolveScope(String scope, String conversationId,
                                   String applicationId, String userScopeId) {
            return switch (StringUtils.defaultString(scope)) {
                case "user" -> userScopeId != null
                        ? new Scope("user", userScopeId, applicationId)
                        : new Scope("conversation", conversationId, null);
                case "application" -> StringUtils.isNotBlank(applicationId)
                        ? new Scope("application", applicationId, null)
                        : new Scope("conversation", conversationId, null);
                default -> new Scope("conversation", conversationId, null);
            };
        }

        /**
         * user 档 scope_id = 裸 userId；per-app 隔离由独立的 application_id 列承担（见 resolveScope）。
         * 仅登录用户（type=USER）且有应用上下文成立；匿名无跨对话稳定身份 → 返回 null，喜好落回 conversation。
         */
        private String userScopeId(WorkFlowManage workFlowManage, String applicationId) {
            if (StringUtils.isBlank(applicationId)) {
                return null;
            }
            JsonObject user = toJsonObject(workFlowManage.getParams().get("user"));
            if (user == null || !"USER".equals(user.getString("type"))) {
                return null;
            }
            String userId = user.getString("id");
            return StringUtils.isBlank(userId) ? null : userId;
        }

        private LocalDateTime parseCursor(String value) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            try {
                return LocalDateTime.parse(value);
            } catch (Exception e) {
                log.warn("context-save 游标时间解析失败: {}", value);
                return null;
            }
        }

        private Object readValue(WorkFlowManage workFlowManage, List<String> variablePath) {
            if (variablePath == null || variablePath.isEmpty()) {
                return null;
            }
            return workFlowManage.getContextVariable(variablePath);
        }

        @SuppressWarnings("unchecked")
        private static JsonObject toJsonObject(Object value) {
            if (value instanceof JsonObject json) {
                return json;
            }
            if (value instanceof Map<?, ?> map) {
                return new JsonObject((Map<String, Object>) map);
            }
            return null;
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
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData", new JsonObject());
        NodeData data = new NodeData();
        data.setSummaryReference(toStringList(jsonObject.getJsonArray("summaryReference")));
        data.setFactsReference(toStringList(jsonObject.getJsonArray("factsReference")));
        return data;
    }

    private static List<String> toStringList(JsonArray array) {
        if (array == null) {
            return null;
        }
        return array.stream().map(Object::toString).toList();
    }

    @Override
    public NodeResult<ContextSaveNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
