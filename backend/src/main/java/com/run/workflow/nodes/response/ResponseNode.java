package com.run.workflow.nodes.response;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.*;
import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.run.workflow.nodes.response.pojo.ResponseNodeData.ContentType.jsonObject;

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
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

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
            ResponseNodeData jsonResponseNodeData = node.params;
            if (jsonResponseNodeData.getStatus() != null) {
                workFlowManage.write(node, new StatusContent(jsonResponseNodeData.getStatus(), node, (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
            }
            List<ResponseNodeData.Header> headers = jsonResponseNodeData.getHeaders();
            if (CollectionUtils.isNotEmpty(headers)) {
                Map<String, String> params = new HashMap<>();
                for (ResponseNodeData.Header header : headers) {
                    String location = header.getLocation();
                    if (Strings.CS.equals(location, "reference")) {
                        Object contextVariable = workFlowManage.getContextVariable(header.getReference());
                        params.put(header.getField(), contextVariable.toString());
                    } else {
                        params.put(header.getField(), header.getValue());
                    }
                }
                workFlowManage.write(node, new HeadersContent(params, node, (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
            }

            ResponseNodeData.ContentType contentType = jsonResponseNodeData.getContentType();

            if (contentType == ResponseNodeData.ContentType.jsonFields) {
                JsonObject params = new JsonObject();
                for (ResponseNodeData.JsonField parameter : jsonResponseNodeData.getJsonFields()) {
                    String location = parameter.getLocation();
                    if (Strings.CS.equals(location, "reference")) {
                        params.put(parameter.getField(), workFlowManage.getContextVariable(parameter.getReference()));
                    } else {
                        params.put(parameter.getField(), parameter.getValue());
                    }
                }
                workFlowManage.write(node, new JsonFieldsContent(params.getMap(), node, (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));

            }
            if (contentType == jsonObject) {
                ResponseNodeData.JsonObject jsonObject = jsonResponseNodeData.getJsonObject();
                String location = jsonObject.getLocation();
                String json;
                if (Strings.CS.equals(location, "reference")) {
                    json = JacksonUtils.toJson(workFlowManage.getContextVariable(jsonObject.getReference()));
                } else {
                    json = jsonObject.getValue();
                }
                workFlowManage.write(node, new JsonContent(json, node, (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
            }
            if (contentType == ResponseNodeData.ContentType.plainText) {
                ResponseNodeData.PlainText plainText = jsonResponseNodeData.getPlainText();
                String location = plainText.getLocation();
                String text;
                if (Strings.CS.equals(location, "reference")) {
                    text = JacksonUtils.toJson(workFlowManage.getContextVariable(plainText.getReference()));
                } else {
                    text = plainText.getValue();
                }
                workFlowManage.write(node, new TextContent(text, node, (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
            }

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
