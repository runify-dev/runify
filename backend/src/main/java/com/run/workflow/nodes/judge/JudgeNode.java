package com.run.workflow.nodes.judge;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.*;
import com.run.workflow.entity.Edge;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.judge.common.JudgeBranchMatcher;
import com.run.workflow.nodes.judge.pojo.JudgeNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/25  00:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class JudgeNode extends INode<JudgeNode, JudgeNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "judge-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW);

    public JudgeNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public JudgeNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

    public static class Handle implements BiFunction<WorkFlowManage, JudgeNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, JudgeNode node) {
            JudgeNodeData params = node.params;
            JudgeNodeData.JudgeBranch judgeBranch = JudgeBranchMatcher.matchBranch(params, workFlowManage);
            node.status = NodeStatus.SUCCESS;
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .filter(edgeNodeDefaultKeyValue -> {
                        Edge edge = edgeNodeDefaultKeyValue.getKey();
                        return Strings.CS.equals(edge.getSourceNodeId() + "_right" + "_" + judgeBranch.getId() + "_success", edge.getString("sourceAnchorId"));
                    })
                    .map(DefaultKeyValue::getValue)
                    .toList();

        }
    }

    @Override
    public JudgeNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(JudgeNodeData.class);

    }

    @Override
    public NodeResult<JudgeNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
