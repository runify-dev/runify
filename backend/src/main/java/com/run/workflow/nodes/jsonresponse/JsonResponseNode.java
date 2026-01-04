package com.run.workflow.nodes.jsonresponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.jsonresponse.pojo.JsonResponseNodeData;
import com.run.workflow.nodes.start.entity.HttpMeta;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/4  00:18}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class JsonResponseNode extends INode<JsonResponseNode, JsonResponseNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "json-response-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

    public JsonResponseNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public JsonResponseNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, JsonResponseNode, Supplier<List<Node>>> {


        @SneakyThrows
        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, JsonResponseNode node) {
            JsonResponseNodeData jsonResponseNodeData = node.params;
            RoutingContext context = (RoutingContext) workFlowManage.getParams().get("context");
            Boolean chunk = jsonResponseNodeData.getChunk();
            JsonObject params = new JsonObject();
            for (JsonResponseNodeData.Parameter parameter : jsonResponseNodeData.getParameters()) {
                String location = parameter.getLocation();
                if (Strings.CS.equals(location, "reference")) {
                    params.put(parameter.getField(), workFlowManage.getContextVariable(parameter.getReference()));
                } else {
                    params.put(parameter.getField(), parameter.getValue());
                }
            }
            boolean isJsonGenerator = workFlowManage.getParams().containsKey("jsonGenerator");
            if (isJsonGenerator) {
                JsonGenerator jsonGenerator = (JsonGenerator) workFlowManage.getParams().get("jsonGenerator");
                for (Map.Entry<String, Object> param : params) {
                    jsonGenerator.writeFieldName(param.getKey());
                    jsonGenerator.writeRawValue(JacksonUtils.toJson(param.getValue()));
                }
                jsonGenerator.flush();
            } else {
                if (chunk) {
                    context.response().setChunked(true);
                    context.response().putHeader("Content-Type", "application/json; charset=utf-8");
                    JsonFactory factory = new JsonFactory();
                    JsonGenerator generator = factory.createGenerator(new Writer() {
                        @Override
                        public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                            String s = new String(cbuf, off, len);
                            context.response().write(s);
                        }

                        @Override
                        public void flush() throws IOException {

                        }

                        @Override
                        public void close() throws IOException {

                        }
                    });
                    generator.writeStartObject();
                    workFlowManage.getParams().put("jsonGenerator", generator);
                } else {
                    context.end(params.toBuffer());
                }
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
    public JsonResponseNodeData getNodeData(JsonObject params) {
        JsonObject nodeParams = node.getProperties().getJsonObject("nodeData");
        return JacksonUtils.convert(nodeParams.getMap(), JsonResponseNodeData.class);

    }

    @Override
    public NodeResult<JsonResponseNode> _invoke() {
        return new NodeResult<>(new JsonResponseNode.Handle(), this);
    }

}
