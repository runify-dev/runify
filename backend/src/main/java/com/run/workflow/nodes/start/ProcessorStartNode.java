package com.run.workflow.nodes.start;

import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.JsonContent;
import com.run.workflow.message.struct.StatusContent;
import com.run.workflow.nodes.response.ResponseRenderer;
import com.run.workflow.nodes.response.pojo.ResponseNodeData;
import com.run.workflow.nodes.start.entity.HttpMeta;
import com.run.workflow.nodes.start.entity.ProcessorStartNodeData;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProcessorStartNode extends INode<ProcessorStartNode, ProcessorStartNodeData<HttpMeta>> {
    /**
     * 节点类型
     */
    public final static String type = "start-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    public ProcessorStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ProcessorStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ProcessorStartNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ProcessorStartNode node) {
            boolean valid = node.write(workFlowManage);
            if (!valid) {
                // 入参校验失败:错误响应已由 write 输出,不再进入后续节点
                return List::of;
            }
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();

        }
    }


    @Override
    public ProcessorStartNodeData<HttpMeta> getNodeData(JsonObject params) {
        JsonObject nodeParams = node.getProperties().getJsonObject("nodeData");
        String protocol = nodeParams.getString("protocol");
        JsonObject jsonObject = nodeParams.getJsonObject("meta");
        HttpMeta httpMeta = JacksonUtils.convert(jsonObject.getMap(), HttpMeta.class);
        ProcessorStartNodeData<HttpMeta> processorStartNodeData = new ProcessorStartNodeData<>();
        processorStartNodeData.setMeta(httpMeta);
        processorStartNodeData.setProtocol(ProcessorProtocolConstants.valueOf(protocol));
        return processorStartNodeData;
    }

    @Override
    public NodeResult<ProcessorStartNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


    /**
     * 解析并写入 HTTP 入参。
     * 会按每个参数声明的 {@code type/required/many} 做类型转换与校验:
     * 校验通过则把转换后的值写入上下文并返回 {@code true};
     * 校验失败则按 {@link HttpMeta.ErrorResponse} 信封模板输出错误响应(不进入后续节点)并返回 {@code false}。
     */
    public boolean write(WorkFlowManage workFlowManage) {
        HttpMeta meta = params.getMeta();
        List<HttpMeta.Parameter> parameters = meta.getParameters();
        RoutingContext routingContext = (RoutingContext) workFlowManage.getParams().get("context");
        workFlowManage.writeContext(this, "pools", workFlowManage.getParams().get("pools"));

        if ("application/json".equals(meta.getContentType())) {
            if (List.of("POST", "PUT", "DELETE").contains(meta.getMethod())) {
                JsonObject body = routingContext.body().asJsonObject();
                workFlowManage.writeContext(this, "body", body.getMap());
            }
        } else {
            MultiMap formAttributes = routingContext.request().formAttributes();
            List<FileUpload> fileUploads = routingContext.fileUploads();
            for (FileUpload fileUpload : fileUploads) {
                workFlowManage.writeContext(this, fileUpload.name(), fileUpload);
            }
            Map<String, String> collect = formAttributes.entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            workFlowManage.writeContext(this, "formAttributes", collect);
        }

        // 先做类型转换 + 校验,一次性收集所有错误;全部通过后再写入上下文
        List<String> errors = new ArrayList<>();
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (parameters != null) {
            for (HttpMeta.Parameter parameter : parameters) {
                resolveParameter(parameter, routingContext, resolved, errors);
            }
        }

        if (!errors.isEmpty()) {
            String message = String.join("; ", errors);
            // 写入上下文,供错误响应配置以 reference [<开始节点id>, "error"] 引用
            workFlowManage.writeContext(this, "error", message);
            ResponseNodeData errorResponse = resolveErrorResponse(workFlowManage, meta);
            if (errorResponse != null && errorResponse.getContentType() != null) {
                ResponseRenderer.render(workFlowManage, this, errorResponse);
            } else {
                writeDefaultErrorEnvelope(workFlowManage, message);
            }
            end(NodeStatus.SUCCESS);
            return false;
        }

        resolved.forEach((field, value) -> workFlowManage.writeContext(this, field, value));
        end(NodeStatus.SUCCESS);
        return true;
    }

    /**
     * 解析单个参数:取原始值 → required 校验 → 类型转换。
     * 成功写入 {@code resolved};失败追加到 {@code errors}。非必填且缺失时跳过(不写入)。
     */
    private void resolveParameter(HttpMeta.Parameter parameter, RoutingContext routingContext,
                                  Map<String, Object> resolved, List<String> errors) {
        String field = parameter.getField();
        String type = parameter.getType();
        boolean required = Boolean.TRUE.equals(parameter.getRequired());
        boolean many = Boolean.TRUE.equals(parameter.getMany());

        if (Strings.CS.equals(parameter.getLocation(), "query") && many) {
            List<String> raws = routingContext.queryParams().getAll(field);
            if (raws == null || raws.isEmpty()) {
                if (required) {
                    errors.add(missingMessage(field));
                }
                return;
            }
            List<Object> values = new ArrayList<>();
            boolean ok = true;
            for (String raw : raws) {
                try {
                    values.add(coerce(raw, type));
                } catch (RuntimeException e) {
                    errors.add(typeMessage(field, type, raw));
                    ok = false;
                }
            }
            if (ok) {
                resolved.put(field, values);
            }
            return;
        }

        String raw = Strings.CS.equals(parameter.getLocation(), "query")
                ? routingContext.queryParams().get(field)
                : routingContext.pathParam(field);
        if (raw == null || raw.isBlank()) {
            if (required) {
                errors.add(missingMessage(field));
            }
            return;
        }
        try {
            resolved.put(field, coerce(raw, type));
        } catch (RuntimeException e) {
            errors.add(typeMessage(field, type, raw));
        }
    }

    /**
     * 按声明类型把字符串入参转成目标类型。转换失败抛异常,由调用方捕获。
     * uuid 仅做格式校验、仍写回 String(避免与下游数据库列绑定期望不一致);
     * 未知/空类型按 string 原样处理(向后兼容)。
     */
    private static Object coerce(String raw, String type) {
        if (type == null) {
            return raw;
        }
        String trimmed = raw.trim();
        return switch (type.toLowerCase()) {
            case "integer" -> Integer.valueOf(trimmed);
            case "long" -> Long.valueOf(trimmed);
            case "double" -> Double.valueOf(trimmed);
            case "uuid" -> {
                UUID.fromString(trimmed);
                yield trimmed;
            }
            default -> raw;
        };
    }

    private static String missingMessage(String field) {
        return "参数 " + field + " 必填";
    }

    private static String typeMessage(String field, String type, String raw) {
        return "参数 " + field + " 期望类型 " + type + ",实际值 '" + raw + "' 无法转换";
    }

    /**
     * 错误响应配置三级决策:
     * source=custom → 本节点 errorResponse;
     * source=global(默认)→ 项目统一异常配置的 validationError 槽;
     * 取不到时返回 null,由调用方走内置兜底。
     */
    private ResponseNodeData resolveErrorResponse(WorkFlowManage workFlowManage, HttpMeta meta) {
        if (Strings.CS.equals(meta.getErrorResponseSource(), "custom")) {
            return meta.getErrorResponse();
        }
        Object projectErrorResponse = workFlowManage.getParams().get("projectErrorResponse");
        if (projectErrorResponse instanceof JsonObject config) {
            JsonObject validationError = config.getJsonObject("validationError");
            if (validationError != null) {
                return JacksonUtils.convert(validationError.getMap(), ResponseNodeData.class);
            }
        }
        return null;
    }

    /**
     * 未配置错误响应时的内置兜底:HTTP 200 + {@code {code:400, message:<message>, data:null}}。
     */
    private void writeDefaultErrorEnvelope(WorkFlowManage workFlowManage, String message) {
        JsonObject body = new JsonObject();
        body.put("code", 400);
        body.put("message", message);
        body.putNull("data");

        String workflowRunId = (String) workFlowManage.getParams().get("workflowRunId");
        workFlowManage.write(this, new StatusContent(200, this, workflowRunId, CommonUtils.uuid7().toString()));
        workFlowManage.write(this, new JsonContent(body.encode(), this, workflowRunId, CommonUtils.uuid7().toString()));
    }
}
