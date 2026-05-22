package com.run.workflow.nodes.contextpush;

import com.run.common.constants.ContentTypeConstants;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.contextpush.entity.ContextPushNodeData;
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
 * 上下文推送节点 - 直接推送 content 元素到目标 JsonArray
 */
public class ContextPushNode extends INode<ContextPushNode, ContextPushNodeData> {
    public final static String type = "context-push-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ContextPushNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ContextPushNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, ContextPushNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ContextPushNode node) {
            List<ContextPushNodeData.ContextPushItem> items = node.params.getItems();

            if (items == null || items.isEmpty()) {
                node.status = NodeStatus.SUCCESS;
                return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
            }

            try {
                for (ContextPushNodeData.ContextPushItem item : items) {
                    if (item.getVariable() == null || item.getVariable().isEmpty()) {
                        continue;
                    }

                    List<String> variablePath = item.getVariable();
                    String variableName = variablePath.get(variablePath.size() - 1);
                    List<Object> targetList = getOrCreateTargetList(workFlowManage, variablePath);

                    // 顶层引用模式：直接把引用值 push 上去
                    if ("reference".equals(item.getMode()) && item.getReference() != null && !item.getReference().isEmpty()) {
                        Object refValue = workFlowManage.getContextVariable(item.getReference());
                        if (refValue != null) {
                            if (refValue instanceof List<?> list) {
                                targetList.addAll(list);
                            } else if (refValue instanceof JsonArray ja) {
                                targetList.addAll(ja.getList());
                            } else {
                                targetList.add(refValue instanceof JsonObject jo ? jo.getMap() : refValue);
                            }
                        }
                        writeTargetList(workFlowManage, variablePath.getFirst(), variableName, targetList);
                        continue;
                    }

                    // 自定义模式：构建 content 元素
                    JsonObject contentElement = buildContentElement(item, workFlowManage);
                    if (contentElement == null) {
                        continue;
                    }

                    // 追加 content 元素
                    targetList.add(contentElement.getMap());

                    // 写回
                    writeTargetList(workFlowManage, variablePath.getFirst(), variableName, targetList);
                }

                node.status = NodeStatus.SUCCESS;
            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
            }

            return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }

        /**
         * 获取目标变量的 List，不存在则新建
         */
        @SuppressWarnings("unchecked")
        private List<Object> getOrCreateTargetList(WorkFlowManage workFlowManage, List<String> variablePath) {
            Object existing = workFlowManage.getContextVariable(variablePath);
            if (existing instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            return new ArrayList<>();
        }

        /**
         * 写回目标数组（支持循环上下文）
         */
        private void writeTargetList(WorkFlowManage workFlowManage, String nodeId, String variableName, List<Object> list) {
            if (List.of(WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP_LOOP).contains(workFlowManage.getWorkFlow().getWorkflowType())) {
                LoopWorkFlowManage loopWorkFlowManage = (LoopWorkFlowManage) workFlowManage;
                loopWorkFlowManage.writeLoopContext(nodeId, variableName, list);
            } else {
                workFlowManage.writeContext(nodeId, variableName, list);
            }
        }

        /**
         * 构建 content 元素（与 start-node 保持一致的格式）
         */
        private JsonObject buildContentElement(ContextPushNodeData.ContextPushItem item, WorkFlowManage workFlowManage) {
            String content;

            if ("reference".equals(item.getMode())) {
                if (item.getReference() != null && !item.getReference().isEmpty()) {
                    Object refValue = workFlowManage.getContextVariable(item.getReference());
                    if (refValue instanceof JsonObject v) {
                        if (v.containsKey("type")) {
                            if (ContentTypeConstants.contains(v.getString("type"))) {
                                return v;
                            }
                        }
                    }
                    content = refValue != null ? refValue.toString() : null;
                } else {
                    return null;
                }
            } else {
                content = workFlowManage.generatePrompt(item.getContent());
            }

            if (StringUtils.isEmpty(content)) {
                return null;
            }

            ContentTypeConstants contentType = switch (item.getRole()) {
                case "user" -> ContentTypeConstants.QUESTION;
                case "system" -> ContentTypeConstants.SYSTEM;
                case "assistant" -> ContentTypeConstants.TEXT;
                case "tool" -> ContentTypeConstants.TOOL;
                default -> ContentTypeConstants.TEXT;
            };

            JsonObject element = new JsonObject();
            element.put("type", contentType.name());

            if ("tool".equals(item.getRole())) {
                try {
                    Map<String, Object> toolContent = JacksonUtils.fromJson(content, new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
                    element.put("id", CommonUtils.uuid7().toString());
                    element.put("functionName", toolContent.getOrDefault("toolName", ""));
                    element.put("arguments", toolContent.getOrDefault("functionArguments", "{}"));
                    element.put("content", toolContent.getOrDefault("content", ""));
                } catch (Exception e) {
                    element.put("content", content);
                }
            } else {
                element.put("content", content);
            }

            return element;
        }
    }

    @Override
    public ContextPushNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ContextPushNodeData data = new ContextPushNodeData();

        JsonArray itemsArray = jsonObject.getJsonArray("items");
        if (itemsArray != null) {
            List<ContextPushNodeData.ContextPushItem> items = new ArrayList<>();
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject itemObj = itemsArray.getJsonObject(i);
                ContextPushNodeData.ContextPushItem item = new ContextPushNodeData.ContextPushItem();

                if (itemObj.getJsonArray("variable") != null) {
                    item.setVariable(itemObj.getJsonArray("variable").stream()
                            .map(Object::toString)
                            .toList());
                }

                item.setMode(itemObj.getString("mode"));

                if (itemObj.getJsonArray("reference") != null) {
                    item.setReference(itemObj.getJsonArray("reference").stream()
                            .map(Object::toString)
                            .toList());
                }

                item.setContent(itemObj.getString("content"));
                item.setRole(itemObj.getString("role"));

                items.add(item);
            }
            data.setItems(items);
        }

        return data;
    }

    @Override
    public NodeResult<ContextPushNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
