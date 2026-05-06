package com.run.workflow.nodes.loopcontinue;

import com.run.common.util.CommonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.common.LoopConditionMatcher;
import com.run.workflow.message.struct.BreakContent;
import com.run.workflow.message.struct.ContinueContent;
import com.run.workflow.nodes.loopcontinue.entity.LoopContinueNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 循环跳过节点（continue）
 * 当条件满足时，跳过当前循环迭代的剩余节点，进入下一次迭代
 * 条件不满足时，正常执行下一个节点
 */
public class LoopContinueNode extends INode<LoopContinueNode, LoopContinueNodeData> {

    public final static String type = "loop-continue-node";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );


    public LoopContinueNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public LoopContinueNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, LoopContinueNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, LoopContinueNode node) {
            LoopContinueNodeData params = node.params;

            boolean hasConditions = params.getConditions() != null && !params.getConditions().isEmpty();
            boolean matched = !hasConditions || LoopConditionMatcher.matchConditions(
                    params.getConditions(),
                    params.getLogic(),
                    workFlowManage
            );

            node.end(NodeStatus.SUCCESS);

            if (matched) {
                workFlowManage.write(node, new ContinueContent(Boolean.TRUE, node, (String) workFlowManage.getParams().get("workflowRunId"), CommonUtils.uuid7().toString()));
                return () -> List.of();
            }
            workFlowManage.write(node, new ContinueContent(Boolean.FALSE, node, (String) workFlowManage.getParams().get("workflowRunId"), CommonUtils.uuid7().toString()));
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(com.run.common.keyvalue.DefaultKeyValue::getValue)
                    .toList();
        }
    }

    @Override
    public LoopContinueNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(LoopContinueNodeData.class);
    }

    @Override
    public NodeResult<LoopContinueNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
