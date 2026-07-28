package com.run.workflow.nodes.start;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.start.entity.ToolStartNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 工具工作流开始节点。
 * 从执行器注入的 params 里取 {@code input}(调用参数) 与 {@code config}(配置)，写入上下文：
 * <ul>
 *   <li>input 的每个字段写到 start-node 顶层，供下游以 [start-node, field] 引用</li>
 *   <li>config 整体写到 start-node.config，secret 值已解密，绝不对 LLM 暴露</li>
 * </ul>
 */
public class ToolStartNode extends INode<ToolStartNode, ToolStartNodeData> {
    public final static String type = "start-node";
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.TOOL);

    public ToolStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ToolStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ToolStartNode, Supplier<List<Node>>> {
        @Override
        @SuppressWarnings("unchecked")
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ToolStartNode node) {
            Object input = workFlowManage.getParams().get("input");
            if (input instanceof Map<?, ?> inputMap) {
                for (Map.Entry<?, ?> entry : inputMap.entrySet()) {
                    workFlowManage.writeContext(node, String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            Object config = workFlowManage.getParams().get("config");
            workFlowManage.writeContext(node, "config", config instanceof Map ? config : Map.of());
            // 调用方上下文（谁调用谁知道）：下游可引用 start-node.caller.type 等
            Object caller = workFlowManage.getParams().get("caller");
            workFlowManage.writeContext(node, "caller", caller instanceof Map ? caller : Map.of());
            node.end(NodeStatus.SUCCESS);
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();
        }
    }

    @Override
    public ToolStartNodeData getNodeData(JsonObject params) {
        return new ToolStartNodeData();
    }

    @Override
    public NodeResult<ToolStartNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
