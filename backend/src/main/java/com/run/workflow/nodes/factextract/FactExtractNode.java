package com.run.workflow.nodes.factextract;

import com.run.common.constants.ContentTypeConstants;
import com.run.workflow.INode;
import com.run.workflow.LoopWorkFlowManage;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import com.run.workflow.nodes.contextmanage.service.SectionRegistry;
import com.run.workflow.nodes.contextmanage.summarizer.FactExtractor;
import com.run.workflow.nodes.factextract.entity.FactExtractNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * AI 便签提取节点：读一段消息 → AI 抽便签（convention/preference/env/goal/todo）→ 合并写回 facts 变量。
 * <p>
 * 与 context-manage 分工：context-manage 只做压缩、产出 messages；便签的语义提取交给本节点，
 * 时机由画布位置决定（用户问题进来放一个抽目标/约定、循环里放一个持续抽待办都行）。
 * 抽到的便签写进 facts 变量，最终由 context-manage 读入并渲染成 messages 顶部的便签旁注——
 * 主干 messages 仍由 context-manage 单独产出，ai-chat 照旧直接取 messages。
 * <p>
 * 契约：任何异常放行——facts 变量保持原值、节点成功、不阻断工作流；唯一外部调用是 LLM 抽取。
 */
@Slf4j
public class FactExtractNode extends INode<FactExtractNode, FactExtractNodeData> {
    public final static String type = "fact-extract-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    /**
     * 喂给 LLM 的片段长度上限（字符），防止抽取调用本身爆预算
     */
    private static final int SEGMENT_MAX_CHARS = 24000;

    public FactExtractNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public FactExtractNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, FactExtractNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, FactExtractNode node) {
            List<ContextService.Fact> existing = readFacts(workFlowManage, node.params.getFactsVariable());
            // 便签子区定义来自应用级"便签设置"（内置+自定义统一入库，description/listStyle 均库驱动）；
            // 节点的 factSections 是"在应用已有子区里挑哪些抽"的选择（为空=全部 enabled）。
            String applicationId = Objects.toString(workFlowManage.getParams().get("applicationId"), null);
            List<String> selected = node.params.getFactSections();
            boolean selectAll = selected == null || selected.isEmpty();
            List<ContextService.SectionDef> sections = new ArrayList<>();
            for (SectionRegistry.Section s : SectionRegistry.load(applicationId)) {
                if (!s.enabled() || (!selectAll && !selected.contains(s.key()))) {
                    continue;
                }
                sections.add(new ContextService.SectionDef(s.key(), s.label(), s.description(), s.listStyle()));
            }
            List<ContextService.Fact> extracted = List.of();
            try {
                Iterable<?> sourceList = readList(workFlowManage, node.params.getSourceReference());
                String segment = renderSegment(sourceList);

                if (StringUtils.isNotBlank(segment) && StringUtils.isNotBlank(node.params.getModelId())) {
                    extracted = FactExtractor.extract(node.params.getModelId(),
                            StringUtils.defaultIfBlank(node.params.getMethod(), "fc"),
                            StringUtils.abbreviate(segment, SEGMENT_MAX_CHARS), sections);
                }

                // 同键覆盖累积（本轮抽到的在后，后写覆盖先写）
                List<Map<String, Object>> merged = mergeFacts(existing, extracted);
                writeVariable(workFlowManage, node.params.getFactsVariable(), merged);
                workFlowManage.writeContext(node, "facts", merged);
                workFlowManage.writeContext(node, "extracted", extracted.size());
                node.status = NodeStatus.SUCCESS;
            } catch (Throwable e) {
                // 出错放行：facts 变量保持原值，节点成功，不阻断
                log.warn("fact-extract 抽取失败，放行原值", e);
                workFlowManage.writeContext(node, "facts", factsToMaps(existing));
                workFlowManage.writeContext(node, "extracted", 0);
                node.status = NodeStatus.SUCCESS;
            }
            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        /**
         * 把消息列表渲染成供 LLM 抽取的片段原文（用户/助手/工具，按类型标注）
         */
        private String renderSegment(Iterable<?> elements) {
            StringBuilder sb = new StringBuilder();
            for (Object element : elements) {
                JsonObject json = toJsonObject(element);
                if (json == null) {
                    continue;
                }
                ContentTypeConstants type = parseType(json);
                if (type == null) {
                    continue;
                }
                switch (type) {
                    case QUESTION -> append(sb, "用户: ", json.getString("content", ""));
                    case TEXT -> append(sb, "助手: ", json.getString("content", ""));
                    case SYSTEM -> append(sb, "系统: ", json.getString("content", ""));
                    case TOOL -> {
                        String tool = json.getString("toolName", "?");
                        String args = StringUtils.abbreviate(json.getString("functionArguments", ""), 200);
                        String out = StringUtils.abbreviate(json.getString("content", ""), 400);
                        append(sb, "", "工具 " + tool + " 参数:" + args + " 结果:" + out);
                    }
                    default -> {
                    }
                }
            }
            return sb.toString().strip();
        }

        private void append(StringBuilder sb, String prefix, String content) {
            if (StringUtils.isBlank(content)) {
                return;
            }
            sb.append(prefix).append(content.strip()).append("\n");
        }

        private List<ContextService.Fact> readFacts(WorkFlowManage workFlowManage, List<String> variablePath) {
            List<ContextService.Fact> facts = new ArrayList<>();
            for (Object element : readList(workFlowManage, variablePath)) {
                JsonObject json = toJsonObject(element);
                if (json == null) {
                    continue;
                }
                String subtype = json.getString("subtype");
                String key = json.getString("key");
                String value = json.getString("value");
                if (StringUtils.isNotBlank(subtype) && StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    facts.add(new ContextService.Fact(subtype, key, value));
                }
            }
            return facts;
        }

        /**
         * 便签合并：同 (subtype, key) 覆盖，本轮新增在后（后写覆盖先写）
         */
        private List<Map<String, Object>> mergeFacts(List<ContextService.Fact> existing,
                                                     List<ContextService.Fact> added) {
            Map<String, ContextService.Fact> merged = new LinkedHashMap<>();
            for (ContextService.Fact fact : existing) {
                merged.put(fact.subtype() + "|" + fact.key(), fact);
            }
            for (ContextService.Fact fact : added) {
                merged.put(fact.subtype() + "|" + fact.key(), fact);
            }
            return merged.values().stream().map(FactExtractNode.Handle::factToMap).toList();
        }

        private List<Map<String, Object>> factsToMaps(List<ContextService.Fact> facts) {
            return facts.stream().map(FactExtractNode.Handle::factToMap).toList();
        }

        private static Map<String, Object> factToMap(ContextService.Fact f) {
            return new LinkedHashMap<>(Map.of("subtype", f.subtype(), "key", f.key(), "value", f.value()));
        }

        /**
         * 写便签变量（父层变量，跨迭代持久；支持循环上下文向上委托）
         */
        private void writeVariable(WorkFlowManage workFlowManage, List<String> variablePath, Object value) {
            if (variablePath == null || variablePath.size() < 2) {
                return;
            }
            String nodeId = variablePath.getFirst();
            String variableName = variablePath.get(variablePath.size() - 1);
            if (List.of(WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP_LOOP)
                    .contains(workFlowManage.getWorkFlow().getWorkflowType())) {
                ((LoopWorkFlowManage) workFlowManage).writeLoopContext(nodeId, variableName, value);
            } else {
                workFlowManage.writeContext(nodeId, variableName, value);
            }
        }

        private Iterable<?> readList(WorkFlowManage workFlowManage, List<String> variablePath) {
            if (variablePath == null || variablePath.isEmpty()) {
                return List.of();
            }
            Object value = workFlowManage.getContextVariable(variablePath);
            return switch (value) {
                case List<?> list -> list;
                case JsonArray array -> array;
                // 单条 Content（如 start-node.question 就是当前用户消息本身）也当作单元素列表处理，
                // 否则接了"用户问题"这种单对象变量会被当空、抽不到当前这句
                case JsonObject jo -> jo.getString("type") != null ? List.of(jo) : List.of();
                case Map<?, ?> map -> map.get("type") != null ? List.of(map) : List.of();
                case null, default -> List.of();
            };
        }

        private static ContentTypeConstants parseType(JsonObject content) {
            String type = content.getString("type");
            if (StringUtils.isBlank(type) || !ContentTypeConstants.contains(type)) {
                return null;
            }
            return ContentTypeConstants.valueOf(type);
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
    }

    @Override
    public FactExtractNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData", new JsonObject());
        FactExtractNodeData data = new FactExtractNodeData();
        data.setSourceReference(toStringList(jsonObject.getJsonArray("sourceReference")));
        data.setFactsVariable(toStringList(jsonObject.getJsonArray("factsVariable")));
        data.setModelId(jsonObject.getString("modelId"));
        data.setMethod(jsonObject.getString("method"));
        data.setFactSections(toStringList(jsonObject.getJsonArray("factSections")));
        return data;
    }

    private static List<String> toStringList(JsonArray array) {
        if (array == null) {
            return null;
        }
        return array.stream().map(Object::toString).toList();
    }

    @Override
    public NodeResult<FactExtractNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
