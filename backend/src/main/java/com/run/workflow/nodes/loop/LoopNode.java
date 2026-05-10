package com.run.workflow.nodes.loop;

import com.run.common.function.Write;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.entity.NodeSerialize;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.*;
import com.run.workflow.nodes.loop.entity.LoopNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 循环节点（异步模式，与 AI 节点一致）
 * <p>
 * Handle.apply() 返回 null，内部驱动迭代。
 * 每次迭代创建子 WorkFlowManage，通过 Write 包装器感知子工作流结束，
 * 然后决定继续下一次迭代还是退出循环，最后调用 nextInvoke 进入后续节点。
 */
@Slf4j
public class LoopNode extends INode<LoopNode, LoopNodeData> {

    public final static String type = "loop-node";

    private static final String LOOP_START_NODE_ID = "loop-start-node";
    private static final String LOOP_CONTEXT_KEY = "loopContext";
    private static final String ITEM_KEY = "item";
    private static final String INDEX_KEY = "index";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.CHAT_WORKFLOW_LOOP
    );


    public LoopNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public LoopNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, LoopNode, Supplier<List<Node>>> {

        private List<Node> getNextNodes(WorkFlowManage parentWm, LoopNode node) {
            return parentWm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage parentWm, LoopNode node) {
            LoopNodeData data = node.params;

            if (data == null || data.getChildren() == null
                    || data.getChildren().getNodes() == null
                    || data.getChildren().getNodes().isEmpty()) {
                node.end(NodeStatus.SUCCESS);
                return () -> getNextNodes(parentWm, node);
            }

            List<Node> children = data.getChildren().getNodes();
            List<com.run.workflow.entity.Edge> edges = data.getChildren().getEdges();
            WorkflowType loopWfType = resolveLoopWorkflowType(parentWm);

            WorkFlow subWorkFlow = new WorkFlow(children, edges, loopWfType);

            Node startNode = subWorkFlow.getNode(LOOP_START_NODE_ID);
            if (startNode == null) {
                log.warn("循环体中未找到 {}，跳过循环", LOOP_START_NODE_ID);
                node.end(NodeStatus.SUCCESS);
                return () -> getNextNodes(parentWm, node);
            }
            if (data.getLoopVariables() != null) {
                for (LoopNodeData.LoopVariable lv : data.getLoopVariables()) {
                    if (lv.getName() != null && !lv.getName().isEmpty()) {
                        Object defaultValue = convertValue(lv.getDataType(), lv.getDefaultValue());
                        parentWm.writeContext(node, lv.getName(), defaultValue);
                    }
                }
            }

            List<Object> items = resolveItems(data, parentWm);
            Map<String, Object> nodeContent = parentWm.getContext().get(node.getNode().getId());
            Integer index = 0;
            if (nodeContent != null && nodeContent.containsKey("index")) {
                index = (Integer) nodeContent.get("index");
            }
            // 启动第一次迭代（异步驱动）
            iterate(parentWm, node, children, edges, loopWfType, startNode, items, index);

            // 返回 null，由迭代链内部驱动 nextInvoke
            return null;
        }

        public DefaultKeyValue<Function<WorkFlow, Node>, Map<String, Map<String, Object>>> get(Content content, JsonArray context) {
            if (content != null && content.getPosition() != null) {
                HashMap<String, Map<String, Object>> _context = new HashMap<>();
                for (int i = 0; i < context.size(); i++) {
                    NodeSerialize nodeSerialize = context.getJsonObject(i).mapTo(NodeSerialize.class);
                    _context.put(nodeSerialize.getNodeInfo().getId(), nodeSerialize.getContext());
                }
                return new DefaultKeyValue<>(wm -> wm.getNode(content.getPosition().id()), _context);
            } else {
                return new DefaultKeyValue<>((wm) -> wm.getNode(LOOP_START_NODE_ID), new HashMap<>());
            }
        }

        /**
         * 执行一次循环迭代
         */
        private void iterate(WorkFlowManage parentWm, LoopNode node,
                             List<Node> children, List<com.run.workflow.entity.Edge> edges,
                             WorkflowType loopWfType, Node startNode,
                             List<Object> items, int index) {
            if (items != null && index >= items.size()) {
                node.end(NodeStatus.SUCCESS);
                parentWm.nextInvoke(node, () -> getNextNodes(parentWm, node));
                return;
            }
            Object item = items == null ? index : items.get(index);
            AtomicBoolean isBreak = new AtomicBoolean(false);
            Write<WorkFlowManage, INode<?, ?>, Content, Boolean> loopWrite =
                    (wm, iNode, content, done) -> {
                        if (!done) {
                            Position position = content.getPosition();
                            content.setPosition(new Position(position.id(), position.index() == null ? index : position.index(), position.children()));
                            parentWm.writeChildren(node, content, index);
                        }
                        if (content instanceof BreakContent continueContent) {
                            if (continueContent.getContent()) {
                                isBreak.set(true);
                                node.end(NodeStatus.SUCCESS);
                                return;
                            }
                        }
                        if (content instanceof FailureContent failureContent) {
                            isBreak.set(true);
                            node.end(NodeStatus.FAIL);
                            return;
                        }
                        if (content instanceof ApprovalContent approvalContent) {
                            isBreak.set(true);
                            node.end(NodeStatus.SUCCESS);
                            return;
                        }
                        if (done) {
                            List<Map<String, Object>> list = wm.getNodes().stream().map(INode::serialize).map(NodeSerialize::toMap).toList();
                            parentWm.writeContext(node, LOOP_CONTEXT_KEY, list);
                            if (!isBreak.get()) {
                                iterate(parentWm, node, children, edges, loopWfType, startNode, items, index + 1);
                            } else {
                                parentWm.nextInvoke(node, () -> getNextNodes(parentWm, node));
                            }
                        }
                    };
            Map<String, Object> nodeContent = parentWm.getContext().get(node.getNode().getId());
            parentWm.writeContext(node, ITEM_KEY, item);
            parentWm.writeContext(node, INDEX_KEY, index);
            Content c = null;
            HashMap<String, Object> params = new HashMap<>(parentWm.getParams());
            JsonObject content = (JsonObject) parentWm.getParams().get("content");
            params.remove("content");
            JsonArray context = new JsonArray();
            if (content != null) {
                c = ContentConverter.of(content, (String) parentWm.getParams().get("workflowRunId"));
                if (c.getPosition() != null) {
                    if (c.getPosition().index() == index) {
                        if (nodeContent != null) {
                            context = new JsonArray((List) nodeContent.get("loopContext"));
                        }
                        c.setPosition(c.getPosition().children());
                        params.put("content", JsonObject.mapFrom(c));
                    } else {
                        c = null;
                    }
                } else {
                    c = null;
                }
            }

            DefaultKeyValue<Function<WorkFlow, Node>, Map<String, Map<String, Object>>> functionMapDefaultKeyValue = get(c, context);
            WorkFlow subWorkFlow = new WorkFlow(children, edges, loopWfType);
            WorkFlowManage subWm = new LoopWorkFlowManage(
                    subWorkFlow,
                    params,
                    functionMapDefaultKeyValue.getValue(),
                    loopWrite, parentWm, wm -> wm.getContext().computeIfAbsent(node.node.getId(), k -> new HashMap<>()),
                    functionMapDefaultKeyValue.getKey());
            try {
                subWm.invoke();
            } catch (Exception e) {
                log.error("循环第 {} 次迭代执行异常: {}", index, e.getMessage(), e);
                node.end(NodeStatus.FAIL);
                parentWm.nextInvoke(node, () -> getNextNodes(parentWm, node));
            }
        }

        private WorkflowType resolveLoopWorkflowType(WorkFlowManage parentWm) {
            WorkflowType parentType = parentWm.getWorkFlow().getWorkflowType();
            if (parentType == WorkflowType.PROCESSOR_HTTP || parentType == WorkflowType.PROCESSOR_HTTP_LOOP) {
                return WorkflowType.PROCESSOR_HTTP_LOOP;
            }
            return WorkflowType.CHAT_WORKFLOW_LOOP;
        }

        private Object convertValue(String dataType, Object value) {
            if (value == null) {
                return null;
            }
            String strValue = value.toString();
            switch (dataType) {
                case "string":
                    return strValue;
                case "number":
                    try {
                        if (strValue.contains(".")) {
                            return Double.parseDouble(strValue);
                        }
                        return Long.parseLong(strValue);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                case "boolean":
                    return Boolean.parseBoolean(strValue);
                case "array":
                case "dict":
                    try {
                        return com.run.common.util.JacksonUtils.fromJson(strValue, Object.class);
                    } catch (Exception e) {
                        return strValue;
                    }
                default:
                    return strValue;
            }
        }

        private List<Object> resolveItems(LoopNodeData data, WorkFlowManage wm) {
            String loopType = data.getLoopType();

            if ("count".equals(loopType)) {
                int count = data.getLoopCount() != null ? data.getLoopCount() : 0;
                List<Object> items = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    items.add(i);
                }
                return items;
            }

            if ("infinite".equals(loopType)) {
                return null;
            }

            // foreach
            if (data.getLoopVariable() != null && !data.getLoopVariable().isEmpty()) {
                Object variable = wm.getContextVariable(data.getLoopVariable());
                if (variable instanceof Collection<?> collection) {
                    return new ArrayList<>(collection);
                }
                if (variable != null) {
                    return List.of(variable);
                }
            }

            return List.of();
        }
    }

    @Override
    public LoopNodeData getNodeData(JsonObject params) {
        JsonObject nodeData = node.getProperties().getJsonObject("nodeData");
        if (nodeData == null) {
            return new LoopNodeData();
        }
        return nodeData.mapTo(LoopNodeData.class);
    }

    @Override
    public JsonObject getContext() {
        JsonObject context = super.getContext();
        return context;
    }

    @Override
    public NodeResult<LoopNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
