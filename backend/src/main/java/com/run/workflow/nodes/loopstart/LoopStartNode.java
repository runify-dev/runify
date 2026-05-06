package com.run.workflow.nodes.loopstart;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.loopstart.entity.LoopStartNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 循环开始节点
 * 在循环体子画布中，每次迭代开始时自动执行
 * 将当前项(item)和索引(index)写入上下文
 */
public class LoopStartNode extends INode<LoopStartNode, LoopStartNodeData> {

    public final static String type = "loop-start-node";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public LoopStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public LoopStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, LoopStartNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, LoopStartNode node) {
            Object item = workFlowManage.getParams().get("item");
            Object index = workFlowManage.getParams().get("index");
            workFlowManage.writeContext(node, "item", item);
            workFlowManage.writeContext(node, "index", index);

            node.end(NodeStatus.SUCCESS);

            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();
        }
    }

    @Override
    public LoopStartNodeData getNodeData(JsonObject params) {
        return new LoopStartNodeData();
    }

    @Override
    public NodeResult<LoopStartNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
