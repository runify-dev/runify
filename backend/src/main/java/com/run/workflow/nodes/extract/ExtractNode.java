package com.run.workflow.nodes.extract;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.JacksonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.extract.entity.ExtractNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ExtractNode extends INode<ExtractNode, ExtractNodeData> {

    public final static String type = "extract-node";
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP, WorkflowType.PROCESSOR_HTTP_LOOP);

    public ExtractNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ExtractNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, ExtractNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ExtractNode node) {
            if (node.params.getSourceReference() == null || node.params.getSourceReference().isEmpty()) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, new RuntimeException("未指定源变量"));
                return null;
            }

            Object source = workFlowManage.getContextVariable(node.params.getSourceReference());
            if (source == null) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, new RuntimeException("源变量为空"));
                return null;
            }

            // 转为 JSON 字符串供 JsonPath 解析
            String json;
            if (source instanceof String s) {
                json = s;
            } else if (source instanceof JsonObject jo) {
                json = jo.encode();
            } else {
                json = JacksonUtils.toJson(source);
            }

            List<ExtractNodeData.ExtractRule> rules = node.params.getRules();
            if (rules == null || rules.isEmpty()) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, new RuntimeException("未配置提取规则"));
                return null;
            }

            List<String> errors = new ArrayList<>();
            Map<String, Object> extracted = new HashMap<>();

            for (ExtractNodeData.ExtractRule rule : rules) {
                if (StringUtils.isEmpty(rule.getName()) || StringUtils.isEmpty(rule.getPath())) {
                    continue;
                }
                try {
                    Object value = JsonPath.read(json, rule.getPath());
                    extracted.put(rule.getName(), value);
                } catch (PathNotFoundException e) {
                    errors.add(rule.getName() + ": 路径未找到 " + rule.getPath());
                } catch (Exception e) {
                    errors.add(rule.getName() + ": " + e.getMessage());
                }
            }

            boolean success = errors.isEmpty();
            if (!success) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, new RuntimeException(errors.toString()));
                return null;
            }
            // 每个提取的字段单独写入节点上下文，下游可直接引用
            for (Map.Entry<String, Object> entry : extracted.entrySet()) {
                workFlowManage.writeContext(node, entry.getKey(), entry.getValue());
            }
            node.status = NodeStatus.SUCCESS;
            return next(workFlowManage, node);
        }

        private Supplier<List<Node>> next(WorkFlowManage wfm, ExtractNode node) {
            return () -> wfm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }
    }

    @Override
    public ExtractNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ExtractNodeData data = new ExtractNodeData();

        if (jsonObject.getJsonArray("sourceReference") != null) {
            data.setSourceReference(jsonObject.getJsonArray("sourceReference").stream().map(Object::toString).toList());
        }

        JsonArray rulesArray = jsonObject.getJsonArray("rules");
        if (rulesArray != null) {
            List<ExtractNodeData.ExtractRule> rules = new ArrayList<>();
            for (int i = 0; i < rulesArray.size(); i++) {
                JsonObject ruleObj = rulesArray.getJsonObject(i);
                ExtractNodeData.ExtractRule rule = new ExtractNodeData.ExtractRule();
                rule.setName(ruleObj.getString("name"));
                rule.setDescription(ruleObj.getString("description"));
                rule.setPath(ruleObj.getString("path"));
                rules.add(rule);
            }
            data.setRules(rules);
        }

        return data;
    }

    @Override
    public NodeResult<ExtractNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
