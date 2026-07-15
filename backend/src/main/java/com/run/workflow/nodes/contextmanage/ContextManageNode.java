package com.run.workflow.nodes.contextmanage;

import com.run.workflow.INode;
import com.run.workflow.LoopWorkFlowManage;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.contextmanage.entity.ContextManageNodeData;
import com.run.workflow.nodes.contextmanage.service.ContextService;
import com.run.workflow.nodes.contextmanage.service.TokenCounter;
import com.run.workflow.nodes.contextmanage.service.SectionRegistry;
import com.run.workflow.nodes.contextmanage.summarizer.impl.PromptSummaryGenerator;
import com.run.workflow.nodes.contextmanage.summarizer.impl.ToolCallSummaryGenerator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 上下文管理节点：纯变换器——运行时零数据库读写。三类数据，每类"初始值 + 变化值"两个口：
 * <pre>
 * 数据      初始值                          变化值
 * 上下文    sourceSeedVariable（输入，必填） sourceVariable（输出：压缩+拼接摘要/便签后的 messages）
 * 摘要      summarySeedVariable（仅首轮兜底）summaryVariable（读+写回，{text,covered,seedCovered}）
 * 便签      factsSeedVariable（仅首轮兜底）  factsVariable（读+写回，含产物 subtype=artifact）
 * </pre>
 * 上下文流向：初始值(待压上下文) → 压缩 + 顶部拼接便签旁注 + 内联摘要 → 写入变化值（同时经 messages 输出口）。
 * 变化值是"出参"、须≠初始值（否则下一轮把压缩产物当输入 → 二次压缩）；ai-chat 直接引用变化值即可。
 * 摘要/便签的变化值仍是"读+写回"的累积态，两者的初始值均可空（仅首轮兜底）。
 * 变化值须指向跨迭代持久的父层变量（循环体子上下文每次迭代重建，节点自身输出跨迭代不保留）。
 * <p>
 * 编排分工：context-query-node（循环外）查库产出各初始值 → 本节点纯压缩 →
 * context-save-node（循环后）把变化值写回库。子 agent 隔离天然成立（每层画布各自变量）。
 * <p>
 * 契约：不写 conversation_message、标记不落库；任何异常放行原文（变化值保持原值），
 * 节点成功，不阻断。唯一外部调用是阈值触发的 LLM 摘要（可关，失败回退抽取式）。
 */
@Slf4j
public class ContextManageNode extends INode<ContextManageNode, ContextManageNodeData> {
    public final static String type = "context-manage-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ContextManageNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ContextManageNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ContextManageNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ContextManageNode node) {
            String applicationId = Objects.toString(workFlowManage.getParams().get("applicationId"), null);
            ContextService.Config config = node.buildConfig(applicationId);
            // 上下文输入只从初始值读时间线；变化值现在是"输出口"（见下方写回），不再当输入读——
            // 否则下一轮会把上一轮写回的压缩产物又当输入压一遍（double compression）。
            List<JsonObject> seedContents = readContents(workFlowManage, node.params.getSourceSeedVariable());
            List<JsonObject> contents = List.of();
            try {
                // 变化值优先，空则从初始值起步（初始值只读，永不被写）
                SummaryState summary = readSummary(workFlowManage, node);
                List<ContextService.Fact> facts = readFacts(workFlowManage, node);

                // LLM 摘要器：仅在超阈值时被调用一次；未开启/未配模型则恒用抽取式。
                // 方法可配：fc（默认，工具调用）| prompt（提示词标签块，兼容无 FC 模型）
                ContextService.Summarizer summarizer = null;
                if (Boolean.TRUE.equals(node.params.getEnableSummarizer())
                        && StringUtils.isNotBlank(node.params.getSummarizerModelId())) {
                    ContextService.Summarizer inner = "prompt".equals(node.params.getSummarizerMethod())
                            ? new PromptSummaryGenerator(node.params.getSummarizerModelId())
                            : new ToolCallSummaryGenerator(node.params.getSummarizerModelId());
                    // 失败会回退抽取式，但必须留下日志，否则无法诊断"为什么摘要是流水账"
                    summarizer = (prev, segment, sections) -> {
                        try {
                            return inner.summarize(prev, segment, sections);
                        } catch (Exception e) {
                            log.warn("context-manage LLM 摘要失败，回退抽取式（model={}, method={}）: {}",
                                    node.params.getSummarizerModelId(),
                                    StringUtils.defaultIfBlank(node.params.getSummarizerMethod(), "fc"),
                                    e.toString());
                            throw e;
                        }
                    };
                }

                ContextService service = new ContextService(TokenCounter.of(config.tokenEncoding()), summarizer);
                ContextService.Result result = service.process(
                        new ContextService.Input(seedContents, contents, facts,
                                summary.text(), summary.covered()), config);

                // ── 变化值合并写回：便签同键覆盖累积（含产物 artifact 子区） ──
                List<Map<String, Object>> mergedFacts = mergeFacts(facts, result.factsToWrite());
                Map<String, Object> newSummary = new LinkedHashMap<>();
                newSummary.put("text", StringUtils.defaultString(result.summaryText()));
                newSummary.put("covered", result.summaryCovered());
                newSummary.put("seedCovered", result.seedCovered());
                // 游标时间戳：供 context-save 推进 covered_upto（部分覆盖也推进，行边界安全）
                newSummary.put("coveredUpto", StringUtils.defaultString(result.coveredUpto()));

                writeVariable(workFlowManage, node.params.getSummaryVariable(), newSummary);
                writeVariable(workFlowManage, node.params.getFactsVariable(), mergedFacts);
                // 上下文·变化值 = 输出口：把"压缩 + 拼接摘要/便签"后的最终 messages 写回变化值变量，
                // 供 ai-chat 直接引用（与本节点 messages 输出口内容一致）。防呆：变化值须≠初始值。
                writeMessagesOutput(workFlowManage, node, result.messages());

                workFlowManage.writeContext(node, "messages", result.messages());
                workFlowManage.writeContext(node, "summary", newSummary);
                workFlowManage.writeContext(node, "facts", mergedFacts);
                workFlowManage.writeContext(node, "stats", result.stats());
                node.status = NodeStatus.SUCCESS;
            } catch (Throwable e) {
                // 出错放行：投影退化为原文透传（初始值 + 变化值），变化值保持原值，节点成功，不阻断工作流
                log.warn("context-manage 压缩失败，放行原文", e);
                List<Map<String, Object>> passthrough = new ArrayList<>();
                for (JsonObject content : seedContents) {
                    passthrough.add(content.getMap());
                }
                for (JsonObject content : contents) {
                    passthrough.add(content.getMap());
                }
                // 出错放行也要满足严格 user/assistant 交替，否则原文透传照样被模型拒
                List<Map<String, Object>> aligned = ContextService.enforceAlternation(passthrough);
                writeMessagesOutput(workFlowManage, node, aligned);
                workFlowManage.writeContext(node, "messages", aligned);
                workFlowManage.writeContext(node, "summary", Map.of("text", "", "covered", 0, "seedCovered", false));
                workFlowManage.writeContext(node, "facts", List.of());
                workFlowManage.writeContext(node, "stats", Map.of("error", String.valueOf(e.getMessage())));
                node.status = NodeStatus.SUCCESS;
            }
            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        /**
         * 读 Content 列表变量
         */
        private List<JsonObject> readContents(WorkFlowManage workFlowManage, List<String> variablePath) {
            List<JsonObject> result = new ArrayList<>();
            for (Object element : readList(workFlowManage, variablePath)) {
                JsonObject json = toJsonObject(element);
                if (json != null) {
                    result.add(json);
                }
            }
            return result;
        }

        record SummaryState(String text, int covered) {
        }

        /**
         * 摘要：变化值优先；为空（首轮）→ 取初始值（裸字符串或 {text, covered}）
         */
        private SummaryState readSummary(WorkFlowManage workFlowManage, ContextManageNode node) {
            SummaryState current = parseSummary(readValue(workFlowManage, node.params.getSummaryVariable()));
            if (StringUtils.isNotBlank(current.text()) || current.covered() > 0) {
                return current;
            }
            return parseSummary(readValue(workFlowManage, node.params.getSummarySeedVariable()));
        }

        private SummaryState parseSummary(Object value) {
            if (value instanceof String s && StringUtils.isNotBlank(s)) {
                return new SummaryState(s, 0);
            }
            JsonObject json = toJsonObject(value);
            if (json != null) {
                return new SummaryState(json.getString("text", ""),
                        Math.max(0, json.getInteger("covered", 0)));
            }
            return new SummaryState("", 0);
        }

        /**
         * 便签：变化值优先；为空（首轮）→ 取初始值。元素 {subtype, key, value}
         */
        private List<ContextService.Fact> readFacts(WorkFlowManage workFlowManage, ContextManageNode node) {
            List<ContextService.Fact> current = parseFacts(readList(workFlowManage, node.params.getFactsVariable()));
            if (!current.isEmpty()) {
                return current;
            }
            return parseFacts(readList(workFlowManage, node.params.getFactsSeedVariable()));
        }

        private List<ContextService.Fact> parseFacts(Iterable<?> elements) {
            List<ContextService.Fact> facts = new ArrayList<>();
            for (Object element : elements) {
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

        private Object readValue(WorkFlowManage workFlowManage, List<String> variablePath) {
            if (variablePath == null || variablePath.isEmpty()) {
                return null;
            }
            return workFlowManage.getContextVariable(variablePath);
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
            return merged.values().stream()
                    .map(f -> (Map<String, Object>) new LinkedHashMap<String, Object>(
                            Map.of("subtype", f.subtype(), "key", f.key(), "value", f.value())))
                    .toList();
        }

        /**
         * 把压缩+拼接后的 messages 写回上下文·变化值（输出口）。
         * 防呆：变化值未配置则跳过；变化值 == 初始值则跳过并告警——同变量会导致下一轮把压缩产物当输入二次压缩。
         */
        private void writeMessagesOutput(WorkFlowManage workFlowManage, ContextManageNode node, Object messages) {
            List<String> output = node.params.getSourceVariable();
            if (output == null || output.size() < 2) {
                return;
            }
            if (output.equals(node.params.getSourceSeedVariable())) {
                log.warn("context-manage 变化值与初始值指向同一变量，跳过写回以避免二次压缩：{}", output);
                return;
            }
            writeVariable(workFlowManage, output, messages);
        }

        /**
         * 写变化值变量（父层变量，跨迭代持久；支持循环上下文向上委托）
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
                case null, default -> List.of();
            };
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

    private ContextService.Config buildConfig(String applicationId) {
        ContextService.Config defaults = ContextService.Config.defaults();
        ContextManageNodeData data = this.params;

        // 便签子区：与 fact-extract 节点一致——从应用「便签设置」加载（内置+自定义统一入库），
        // 按节点勾选过滤，选择为空 = 纳入全部启用子区。抽取（schema/提示词/解析）与渲染
        // （标题/顺序/列表型限量）共用这一份定义，全部库配置驱动。
        List<String> selected = data.getFactSections();
        boolean selectAll = selected == null || selected.isEmpty();
        List<ContextService.SectionDef> sections = new ArrayList<>();
        for (SectionRegistry.Section s : SectionRegistry.load(applicationId)) {
            if (!s.enabled() || (!selectAll && !selected.contains(s.key()))) {
                continue;
            }
            sections.add(new ContextService.SectionDef(s.key(), s.label(), s.description(), s.listStyle()));
        }

        return new ContextService.Config(
                orDefault(data.getBudget(), defaults.budget()),
                orDefault(data.getHighRatio(), defaults.highRatio()),
                orDefault(data.getLowRatio(), defaults.lowRatio()),
                orDefault(data.getKeepRecentItems(), defaults.keepRecentItems()),
                orDefault(data.getStubAge(), defaults.stubAge()),
                orDefault(data.getTruncAge(), defaults.truncAge()),
                data.getStripMultimodal() == null ? defaults.stripMultimodal() : data.getStripMultimodal(),
                orDefault(data.getReservedTokens(), defaults.reservedTokens()),
                StringUtils.defaultIfBlank(data.getTokenEncoding(), defaults.tokenEncoding()),
                sections);
    }

    private static int orDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static double orDefault(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Override
    public ContextManageNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData", new JsonObject());
        ContextManageNodeData data = new ContextManageNodeData();

        data.setSourceSeedVariable(toStringList(jsonObject.getJsonArray("sourceSeedVariable")));
        data.setSourceVariable(toStringList(jsonObject.getJsonArray("sourceVariable")));
        data.setSummarySeedVariable(toStringList(jsonObject.getJsonArray("summarySeedVariable")));
        data.setSummaryVariable(toStringList(jsonObject.getJsonArray("summaryVariable")));
        data.setFactsSeedVariable(toStringList(jsonObject.getJsonArray("factsSeedVariable")));
        data.setFactsVariable(toStringList(jsonObject.getJsonArray("factsVariable")));
        data.setBudget(jsonObject.getInteger("budget"));
        data.setHighRatio(jsonObject.getDouble("highRatio"));
        data.setLowRatio(jsonObject.getDouble("lowRatio"));
        data.setKeepRecentItems(jsonObject.getInteger("keepRecentItems"));
        data.setStubAge(jsonObject.getInteger("stubAge"));
        data.setTruncAge(jsonObject.getInteger("truncAge"));
        data.setStripMultimodal(jsonObject.getBoolean("stripMultimodal"));
        data.setEnableSummarizer(jsonObject.getBoolean("enableSummarizer"));
        data.setSummarizerModelId(jsonObject.getString("summarizerModelId"));
        data.setSummarizerMethod(jsonObject.getString("summarizerMethod"));
        if (jsonObject.getJsonArray("factSections") != null) {
            data.setFactSections(toStringList(jsonObject.getJsonArray("factSections")));
        }
        data.setReservedTokens(jsonObject.getInteger("reservedTokens"));
        data.setTokenEncoding(jsonObject.getString("tokenEncoding"));
        return data;
    }

    private static List<String> toStringList(JsonArray array) {
        if (array == null) {
            return null;
        }
        return array.stream().map(Object::toString).toList();
    }

    @Override
    public NodeResult<ContextManageNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
