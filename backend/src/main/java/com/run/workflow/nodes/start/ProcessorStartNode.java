package com.run.workflow.nodes.start;

import com.run.common.constants.ProcessorProtocolConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.start.entity.HttpMeta;
import com.run.workflow.nodes.start.entity.ProcessorStartNodeData;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.Map;
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
            node.write(workFlowManage);
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


    public void write(WorkFlowManage workFlowManage) {
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
        for (HttpMeta.Parameter parameter : parameters) {
            String field = parameter.getField();
            if (Strings.CS.equals(parameter.getLocation(), "query")) {
                MultiMap entries = routingContext.queryParams();
                if (parameter.getMany()) {
                    workFlowManage.writeContext(this, field, entries.getAll(field));
                } else {
                    workFlowManage.writeContext(this, field, entries.get(field));
                }
            } else {
                workFlowManage.writeContext(this, field, routingContext.pathParam(field));
            }
        }
        end(NodeStatus.SUCCESS);
    }
}
