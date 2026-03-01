package com.run.workflow.nodes.javascript;


import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.safeapi.CommonAPI;
import com.run.common.util.ConvertValueUtil;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.javascript.pojo.ToolNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  21:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ToolNode extends INode<ToolNode, ToolNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "java-script-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

    public ToolNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ToolNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, ToolNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ToolNode node) {

            ToolNodeData toolNodeData = node.params;
            try (Context context = Context.newBuilder("js").allowHostAccess(HostAccess.EXPLICIT).build()) {
                context.getBindings("js").putMember("api", new CommonAPI());
                context.eval("js", toolNodeData.getCode());
                Value greetFunc = context.getBindings("js")
                        .getMember(toolNodeData.getFunctionName());
                Map<String, Object> input = new HashMap<>();
                for (ToolNodeData.Parameter parameter : toolNodeData.getParameters()) {
                    String location = parameter.getLocation();
                    if (Strings.CS.equals(location, "reference")) {
                        input.put(parameter.getField(), workFlowManage.getContextVariable((List<String>) parameter.getValue()));
                    } else {
                        input.put(parameter.getField(), parameter.getValue());
                    }
                }
                String json = JacksonUtils.toJson(input);
                Value param = context.eval("js", "(" + json + ")");
                Value value = greetFunc.execute(param);
                Object o = ConvertValueUtil.convertValue(value);
                workFlowManage.writeContext(node, "result", o);
                node.status = NodeStatus.SUCCESS;
            }
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();

        }
    }


    @Override
    public ToolNodeData getNodeData(JsonObject params) {
        JsonObject nodeParams = node.getProperties().getJsonObject("nodeData");
        return nodeParams.mapTo(ToolNodeData.class);
    }

    @Override
    public NodeResult<ToolNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }

}
