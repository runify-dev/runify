package com.run.workflow.nodes.response;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.SneakyThrows;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/4  00:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ResponseNode extends INode<ResponseNode, ResponseNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "response-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP, WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP_LOOP);


    public ResponseNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ResponseNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ResponseNode, Supplier<List<Node>>> {


        @SneakyThrows
        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ResponseNode node) {
            ResponseRenderer.render(workFlowManage, node, node.params);
            node.status = NodeStatus.SUCCESS;
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();
        }
    }


    @Override
    public ResponseNodeData getNodeData(JsonObject params) {
        JsonObject nodeParams = node.getProperties().getJsonObject("nodeData");
        return JacksonUtils.convert(nodeParams.getMap(), ResponseNodeData.class);

    }

    @Override
    public NodeResult<ResponseNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }

}
