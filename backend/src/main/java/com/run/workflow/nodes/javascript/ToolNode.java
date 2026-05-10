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
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP);

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
            boolean ioEnabled = Boolean.TRUE.equals(toolNodeData.getAllowIO());
            try (Context context = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.EXPLICIT)
                    .allowAllAccess(ioEnabled)
                    .allowCreateProcess(ioEnabled)
                    .build()) {
                context.getBindings("js").putMember("api", new CommonAPI());

                String code = resolveCode(workFlowManage, toolNodeData);
                boolean isScript = !"function".equals(toolNodeData.getMode());
                Object result;

                if (isScript) {
                    result = executeScript(workFlowManage, toolNodeData, code, context);
                } else {
                    result = executeFunction(workFlowManage, toolNodeData, code, context);
                }

                workFlowManage.writeContext(node, "result", result);
                node.status = NodeStatus.SUCCESS;
            }
            return () -> workFlowManage
                    .getNextList(node.node.getId())
                    .stream()
                    .map(DefaultKeyValue::getValue)
                    .toList();
        }

        /**
         * 解析代码: reference 从上下文取, customize 直接返回
         */
        private String resolveCode(WorkFlowManage workFlowManage, ToolNodeData toolNodeData) {
            String location = toolNodeData.getCodeLocation();
            if ("reference".equals(location)) {
                if (toolNodeData.getCodeReference() != null && !toolNodeData.getCodeReference().isEmpty()) {
                    Object val = workFlowManage.getContextVariable(toolNodeData.getCodeReference());
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return toolNodeData.getCode();
        }

        /**
         * 脚本模式: 参数注入为顶层变量, 代码直接执行
         */
        private Object executeScript(WorkFlowManage workFlowManage, ToolNodeData toolNodeData, String code, Context context) {
            if (toolNodeData.getParameters() != null) {
                for (ToolNodeData.Parameter parameter : toolNodeData.getParameters()) {
                    Object value = resolveParameterValue(workFlowManage, parameter);
                    context.getBindings("js").putMember(parameter.getField(), value);
                }
            }
            Value result = context.eval("js", "(function(){" + code + "})()");
            return ConvertValueUtil.convertValue(result);
        }

        /**
         * 函数模式: 注册函数 -> 按名取函数 -> 传参调用
         */
        private Object executeFunction(WorkFlowManage workFlowManage, ToolNodeData toolNodeData, String code, Context context) {
            context.eval("js", code);
            Value func = context.getBindings("js").getMember(toolNodeData.getFunctionName());
            Map<String, Object> input = new HashMap<>();
            if (toolNodeData.getParameters() != null) {
                for (ToolNodeData.Parameter parameter : toolNodeData.getParameters()) {
                    input.put(parameter.getField(), resolveParameterValue(workFlowManage, parameter));
                }
            }
            String json = JacksonUtils.toJson(input);
            Value param = context.eval("js", "(" + json + ")");
            Value value = func.execute(param);
            return ConvertValueUtil.convertValue(value);
        }

        private Object resolveParameterValue(WorkFlowManage workFlowManage, ToolNodeData.Parameter parameter) {
            String location = parameter.getLocation();
            if (Strings.CS.equals(location, "reference")) {
                return workFlowManage.getContextVariable((List<String>) parameter.getValue());
            }
            return parameter.getValue();
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
