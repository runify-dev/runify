package com.run.workflow.nodes.variableassign;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.variableassign.entity.VariableAssignNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 变量赋值节点
 */
public class VariableAssignNode extends INode<VariableAssignNode, VariableAssignNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "variable-assign-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP,
            WorkflowType.TOOL,
            WorkflowType.TOOL_LOOP
    );

    public VariableAssignNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public VariableAssignNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, VariableAssignNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, VariableAssignNode node) {
            List<VariableAssignNodeData.VariableItem> variables = node.params.getVariables();

            if (variables == null || variables.isEmpty()) {
                node.status = NodeStatus.SUCCESS;
                return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
            }

            try {
                for (VariableAssignNodeData.VariableItem item : variables) {
                    if (item.getVariable() == null || item.getVariable().isEmpty()) {
                        continue;
                    }

                    // 获取变量名（路径最后一段）
                    List<String> variablePath = item.getVariable();
                    String variableName = variablePath.get(variablePath.size() - 1);

                    // 获取要赋的值
                    Object value = getValue(item, workFlowManage);
                    if (List.of(WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP_LOOP, WorkflowType.TOOL_LOOP).contains(workFlowManage.getWorkFlow().getWorkflowType())) {
                        LoopWorkFlowManage loopWorkFlowManage = (LoopWorkFlowManage) workFlowManage;
                        loopWorkFlowManage.writeLoopContext(variablePath.getFirst(), variableName, value);
                    } else {
                        workFlowManage.writeContext(variablePath.getFirst(), variableName, value);
                    }
                }

                node.status = NodeStatus.SUCCESS;
            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                return node.handleFail(workFlowManage, e);
            }

            return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }

        /**
         * 获取要赋的值
         */
        private Object getValue(VariableAssignNodeData.VariableItem item, WorkFlowManage workFlowManage) {
            if ("reference".equals(item.getType())) {
                // 引用模式：从上下文获取值
                if (item.getReference() != null && !item.getReference().isEmpty()) {
                    return workFlowManage.getContextVariable(item.getReference());
                }
                return null;
            } else {
                // 常量模式：根据数据类型转换值
                return convertValue(item.getDataType(), item.getValue());
            }
        }

        /**
         * 根据数据类型转换值
         */
        private Object convertValue(String dataType, Object value) {
            if (value == null) {
                return null;
            }

            String strValue = value.toString();

            switch (dataType) {
                case "string":
                    return strValue;
                case "number":
                    try {
                        if (strValue.contains(".")) {
                            return Double.parseDouble(strValue);
                        }
                        return Long.parseLong(strValue);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                case "boolean":
                    return Boolean.parseBoolean(strValue);
                case "array":
                case "dict":
                    try {
                        return JacksonUtils.fromJson(strValue, Object.class);
                    } catch (Exception e) {
                        return strValue;
                    }
                default:
                    return strValue;
            }
        }
    }

    @Override
    public VariableAssignNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        VariableAssignNodeData data = new VariableAssignNodeData();

        JsonArray variablesArray = jsonObject.getJsonArray("variables");
        if (variablesArray != null) {
            List<VariableAssignNodeData.VariableItem> variables = new ArrayList<>();
            for (int i = 0; i < variablesArray.size(); i++) {
                JsonObject varObj = variablesArray.getJsonObject(i);
                VariableAssignNodeData.VariableItem item = new VariableAssignNodeData.VariableItem();

                if (varObj.getJsonArray("variable") != null) {
                    item.setVariable(varObj.getJsonArray("variable").stream()
                            .map(Object::toString)
                            .toList());
                }

                item.setType(varObj.getString("type"));

                if (varObj.getJsonArray("reference") != null) {
                    item.setReference(varObj.getJsonArray("reference").stream()
                            .map(Object::toString)
                            .toList());
                }

                item.setDataType(varObj.getString("dataType"));
                item.setValue(varObj.getValue("value"));

                variables.add(item);
            }
            data.setVariables(variables);
        }

        return data;
    }

    @Override
    public NodeResult<VariableAssignNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
