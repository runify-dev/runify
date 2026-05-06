package com.run.workflow.nodes.loopbreak;

import com.run.common.util.CommonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.common.LoopConditionMatcher;
import com.run.workflow.message.struct.BreakContent;
import com.run.workflow.nodes.loopbreak.entity.LoopBreakNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 跳出循环节点（break）
 * 当条件满足时，立即跳出整个循环，不再执行后续迭代
 * 条件不满足时，正常执行下一个节点
 */
public class LoopBreakNode extends INode<LoopBreakNode, LoopBreakNodeData> {

    public final static String type = "loop-break-node";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public LoopBreakNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public LoopBreakNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, LoopBreakNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, LoopBreakNode node) {
            LoopBreakNodeData params = node.params;

            boolean hasConditions = params.getConditions() != null && !params.getConditions().isEmpty();
            boolean matched = !hasConditions || LoopConditionMatcher.matchConditions(
                    params.getConditions(),
                    params.getLogic(),
                    workFlowManage
            );

            node.end(NodeStatus.SUCCESS);

            if (matched) {
                workFlowManage.write(node, new BreakContent(Boolean.TRUE, node, (String) workFlowManage.getParams().get("workflowRunId"), CommonUtils.uuid7().toString()));
                return () -> List.of();
            }
            workFlowManage.write(node, new BreakContent(Boolean.FALSE, node, (String) workFlowManage.getParams().get("workflowRunId"), CommonUtils.uuid7().toString()));
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(com.run.common.keyvalue.DefaultKeyValue::getValue)
                    .toList();
        }
    }

    @Override
    public LoopBreakNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(LoopBreakNodeData.class);
    }

    @Override
    public NodeResult<LoopBreakNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
