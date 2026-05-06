package com.run.workflow.nodes.approval;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ApprovalContent;
import com.run.workflow.message.struct.Position;
import com.run.workflow.nodes.approval.entity.ApprovalNodeData;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 审批节点
 * 执行时暂停工作流，返回审批请求给前端，等待用户允许或拒绝
 */
public class ApprovalNode extends INode<ApprovalNode, ApprovalNodeData> {

    public final static String type = "approval-node";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );


    public ApprovalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ApprovalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, ApprovalNode, Supplier<List<Node>>> {
        public boolean current(JsonObject content, ApprovalNode node, Integer index) {
            if (content == null) {
                return false;
            }
            String id = node.getNode().getId();
            JsonObject position = content.getJsonObject("position");
            if (position == null) {
                return false;
            }
            if (index != null) {
                return Strings.CS.equals(position.getString("id"), id) && position.getInteger("index") == index;
            }
            return Strings.CS.equals(position.getString("id"), id);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ApprovalNode node) {
            Integer index = null;
            if (workFlowManage instanceof LoopWorkFlowManage loopWorkFlowManage) {
                index = (Integer) loopWorkFlowManage.getParams().get("index");
            }
            // 检查是否已有审批结果
            JsonObject content = (JsonObject) workFlowManage.getParams().get("content");
            if (!current(content, node, index)) {
                // 没有审批结果 → 发送审批请求，工作流结束
                String prompt = resolvePrompt(node.params, workFlowManage);
                String id = CommonUtils.uuid7().toString();
                node.end(NodeStatus.SUCCESS);
                workFlowManage.write(node, new ApprovalContent(
                        StringUtils.isEmpty(prompt) ? "等待审批" : prompt,
                        NodeStatus.SUCCESS,
                        node,
                        (String) workFlowManage.getParams().get("workflowRunId"),
                        id
                ));
                return () -> List.of();
            }
            String result = content.getString("result");
            workFlowManage.writeContext(node, "approved", result.equals("approve"));
            node.end(NodeStatus.SUCCESS);

            return () -> workFlowManage.getNextList(node.node.getId()).stream()
                    .map(DefaultKeyValue::getValue).toList();
        }

        private String resolvePrompt(ApprovalNodeData nodeData, WorkFlowManage workFlowManage) {
            if (nodeData == null) return null;

            if ("reference".equals(nodeData.getLocation())) {
                if (nodeData.getReference() != null && !nodeData.getReference().isEmpty()) {
                    Object val = workFlowManage.getContextVariable(nodeData.getReference());
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return nodeData.getPrompt();
        }
    }

    @Override
    public ApprovalNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ApprovalNodeData data = new ApprovalNodeData();

        data.setLocation(jsonObject.getString("location"));
        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream()
                    .map(Object::toString)
                    .toList());
        }
        data.setPrompt(jsonObject.getString("prompt"));

        return data;
    }

    @Override
    public NodeResult<ApprovalNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
