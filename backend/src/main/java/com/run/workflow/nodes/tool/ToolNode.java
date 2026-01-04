package com.run.workflow.nodes.tool;


import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.result.Result;
import com.run.common.util.ConvertValueUtil;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.tool.pojo.ToolNodeData;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;
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
    public final static String type = "tool-node";
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
            try (Context context = Context.create()) {
                context.eval("js", toolNodeData.getCode());
                Value greetFunc = context.getBindings("js").getMember(toolNodeData.getFunctionName());
                Value input = context.eval("js", "({})");
                for (ToolNodeData.Parameter parameter : toolNodeData.getParameters()) {
                    String location = parameter.getLocation();
                    if (Strings.CS.equals(location, "reference")) {
                        input.putMember(parameter.getField(), workFlowManage.getContextVariable((List<String>) parameter.getValue()));
                    } else {
                        input.putMember(parameter.getField(), parameter.getValue());
                    }
                }
                Value value = greetFunc.execute(input);
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
