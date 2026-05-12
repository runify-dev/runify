package com.run.workflow.nodes.start;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.openai.request.message.Message;
import com.run.dao.entity.ConversationMessage;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.aggregator.ContentAggregator;
import com.run.workflow.nodes.start.entity.StartNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ChatStartNode extends INode<ChatStartNode, StartNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "start-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW);


    public ChatStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ChatStartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ChatStartNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ChatStartNode node) {
            node.write();

            // 设置全局变量
            if (node.params.getGlobalVariables() != null) {
                for (StartNodeData.GlobalVariable variable : node.params.getGlobalVariables()) {
                    if (variable.getName() != null && !variable.getName().isEmpty()) {
                        Object value = convertValue(variable.getDataType(), variable.getDefaultValue());
                        workFlowManage.writeGlobalContext(variable.getName(), value);
                    }
                }
            }

            JsonArray messages = new JsonArray();
            for (ConversationMessage conversationMessage : node.params.messages.subList(0, node.params.messages.size() - 1)) {
                JsonArray content = conversationMessage.getContent();
                for (Object o : content) {
                    messages.add(o);
                }
            }
            workFlowManage.writeContext(node, "messages", messages);
            workFlowManage.writeContext(node, "question", node.params.messages.getLast().getContent().getJsonObject(0).getString("content"));
            node.end(NodeStatus.SUCCESS);
            return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
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
                        return com.run.common.util.JacksonUtils.fromJson(strValue, Object.class);
                    } catch (Exception e) {
                        return strValue;
                    }
                default:
                    return strValue;
            }
        }
    }


    @Override
    public StartNodeData getNodeData(JsonObject params) {
        List<ConversationMessage> messages = (List<ConversationMessage>) params.getMap().get("messages");
        StartNodeData startNodeData = new StartNodeData();
        startNodeData.setMessages(messages);

        // 解析全局变量
        JsonObject nodeData = node.getProperties().getJsonObject("nodeData");
        if (nodeData != null) {
            JsonArray globalVarsArray = nodeData.getJsonArray("globalVariables");
            if (globalVarsArray != null) {
                List<StartNodeData.GlobalVariable> globalVariables = new ArrayList<>();
                for (int i = 0; i < globalVarsArray.size(); i++) {
                    JsonObject varObj = globalVarsArray.getJsonObject(i);
                    StartNodeData.GlobalVariable variable = new StartNodeData.GlobalVariable();
                    variable.setName(varObj.getString("name"));
                    variable.setLabel(varObj.getString("label"));
                    variable.setDataType(varObj.getString("dataType"));
                    variable.setDefaultValue(varObj.getValue("defaultValue"));
                    globalVariables.add(variable);
                }
                startNodeData.setGlobalVariables(globalVariables);
            }
        }

        return startNodeData;
    }

    @Override
    public NodeResult<ChatStartNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


    public void write() {

    }
}
