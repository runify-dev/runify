package com.run.workflow.nodes.response;

import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.INode;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.NodeStatus;
import com.run.workflow.message.struct.*;
import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/22}
 * {@code @Version 1.0}
 * {@code @注释: 响应渲染:按 ResponseNodeData 配置输出 状态码/响应头/响应体。
 * 供响应节点与开始节点错误响应(入参校验失败)共用。}
 */
public class ResponseRenderer {

    public static void render(WorkFlowManage workFlowManage, INode<?, ?> node, ResponseNodeData data) {
        String workflowRunId = (String) workFlowManage.getParams().get("workflowRunId");
        if (data.getStatus() != null) {
            workFlowManage.write(node, new StatusContent(data.getStatus(), node, workflowRunId, CommonUtils.uuid7().toString()));
        }
        List<ResponseNodeData.Header> headers = data.getHeaders();
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
            workFlowManage.write(node, new HeadersContent(params, node, workflowRunId, CommonUtils.uuid7().toString()));
        }

        ResponseNodeData.ContentType contentType = data.getContentType();

        if (contentType == ResponseNodeData.ContentType.jsonFields) {
            JsonObject params = new JsonObject();
            for (ResponseNodeData.JsonField parameter : data.getJsonFields()) {
                String location = parameter.getLocation();
                if (Strings.CS.equals(location, "reference")) {
                    params.put(parameter.getField(), workFlowManage.getContextVariable(parameter.getReference()));
                } else {
                    // customize 留空(非必填字段)输出 JSON null,而非空字符串
                    Object value = parameter.getValue();
                    boolean blank = value == null || (value instanceof String s && s.isEmpty());
                    params.put(parameter.getField(), blank ? null : coerceLenient(value, parameter.getType()));
                }
            }
            workFlowManage.write(node, new JsonFieldsContent(params.getMap(), node, workflowRunId, CommonUtils.uuid7().toString()));
        }
        if (contentType == ResponseNodeData.ContentType.jsonObject) {
            ResponseNodeData.JsonObject jsonObject = data.getJsonObject();
            String location = jsonObject.getLocation();
            String json;
            if (Strings.CS.equals(location, "reference")) {
                json = JacksonUtils.toJson(workFlowManage.getContextVariable(jsonObject.getReference()));
            } else {
                json = jsonObject.getValue();
            }
            workFlowManage.write(node, new JsonContent(json, node, workflowRunId, CommonUtils.uuid7().toString()));
        }
        if (contentType == ResponseNodeData.ContentType.plainText) {
            ResponseNodeData.PlainText plainText = data.getPlainText();
            String location = plainText.getLocation();
            String text;
            if (Strings.CS.equals(location, "reference")) {
                text = JacksonUtils.toJson(workFlowManage.getContextVariable(plainText.getReference()));
            } else {
                text = workFlowManage.generatePrompt(plainText.getValue());
            }
            workFlowManage.write(node, new TextContent(text, NodeStatus.SUCCESS, node, workflowRunId, CommonUtils.uuid7().toString()));
        }
    }

    /**
     * customize 值按声明类型宽松转换(integer/long/double),让 {@code code: 400} 输出为数字而非字符串。
     * 未声明类型、非字符串值或转换失败时原样返回,不影响响应输出。
     */
    static Object coerceLenient(Object value, String type) {
        if (!(value instanceof String s) || type == null) {
            return value;
        }
        try {
            return switch (type.toLowerCase()) {
                case "integer" -> Integer.valueOf(s.trim());
                case "long" -> Long.valueOf(s.trim());
                case "double" -> Double.valueOf(s.trim());
                default -> value;
            };
        } catch (RuntimeException e) {
            return value;
        }
    }
}
