package com.run.workflow.entity;


import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.WorkflowType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  20:49}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class WorkFlow {

    /**
     * 当前工作流所有节点
     */
    private List<Node> nodes;
    /**
     * 当前工作流所有线
     */
    private List<Edge> edges;
    /**
     * 节点id与节点的Map  方便通过节点id迅速找到节点
     */
    private Map<String, Node> nodeMap;
    /**
     * 节点id与下一个节点的Map  方便通过节点找到当前节点后面的n个节点
     */
    private Map<String, List<DefaultKeyValue<Edge, Node>>> nextNodeMap;
    /**
     * 节点id与上一个节点的Map  方便通过节点找到当前节点前面的n个节点
     */
    private Map<String, List<DefaultKeyValue<Edge, Node>>> upNodeMap;

    private List<NodeField> fieldList;

    private List<NodeField> globalFieldList;

    private WorkflowType workflowType;


    public WorkFlow() {

    }

    public static WorkFlow of(JsonObject workflow, WorkflowType workflowType) {
        WorkFlow workFlow = workflow.mapTo(WorkFlow.class);
        return new WorkFlow(workFlow.nodes, workFlow.edges, workflowType);
    }


    public WorkFlow(List<Node> nodes, List<Edge> edges, WorkflowType workflowType) {
        this.workflowType = workflowType;
        this.nodes = nodes;
        this.edges = edges;
        this.nodeMap = this.nodes.stream()
                .collect(Collectors.toMap(Node::getId, Node::self));
        this.nextNodeMap = this.edges.stream()
                .map(edge -> new DefaultKeyValue<>(edge.getSourceNodeId(), new DefaultKeyValue<>(edge, this.nodeMap.get(edge.getTargetNodeId()))))
                .collect(Collectors.groupingBy(DefaultKeyValue::getKey, Collectors.mapping(DefaultKeyValue::getValue, Collectors.toList())));
        this.upNodeMap = this.edges.stream()
                .map(edge -> new DefaultKeyValue<>(edge.getTargetNodeId(), new DefaultKeyValue<>(edge, this.nodeMap.get(edge.getSourceNodeId()))))
                .collect(Collectors.groupingBy(DefaultKeyValue::getKey, Collectors.mapping(DefaultKeyValue::getValue, Collectors.toList())));
        List<NodeField> nodeFields = new ArrayList<>();
        List<NodeField> globalFieldList = new ArrayList<>();
        for (Node node : this.nodes) {
            JsonObject properties = node.getProperties();
            JsonArray jsonArray = properties.getJsonArray("field_list", new JsonArray());
            JsonArray globalFields = properties.getJsonArray("globalFields", new JsonArray());
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                String label = jsonObject.getString("label");
                String value = jsonObject.getString("value");
                nodeFields.add(new NodeField(node.getId(), properties.getString("name"), label, value));
            }
            for (int i = 0; i < globalFields.size(); i++) {
                JsonObject jsonObject = globalFields.getJsonObject(i);
                String label = jsonObject.getString("label");
                String value = jsonObject.getString("value");
                globalFieldList.add(new NodeField(node.getId(), "global", label, value));
            }
        }
        this.fieldList = nodeFields;
        this.globalFieldList = globalFieldList;
    }

    /**
     * 根据节点id获取上n个节点 以及当前节点与上一个节点的连接线
     *
     * @param nodeId 节点id
     * @return 上n个节点
     */
    public List<DefaultKeyValue<Edge, Node>> getUpNodes(String nodeId) {
        return this.upNodeMap.getOrDefault(nodeId, List.of());
    }

    /**
     * 根据节点id获取下n个节点 以及当前节点与上一个节点的连接线
     *
     * @param nodeId 节点id
     * @return 下n个节点
     */
    public List<DefaultKeyValue<Edge, Node>> getNextNodes(String nodeId) {
        return this.nextNodeMap.getOrDefault(nodeId, List.of());
    }

    /**
     * 根据节点id 获取节点
     *
     * @param nodeId 节点id
     * @return 节点对象
     */
    public Node getNode(String nodeId) {
        return this.nodeMap.get(nodeId);
    }

    public String resetPrompt(String prompt) {
        for (NodeField field : this.fieldList) {
            prompt = field.resetVariable(prompt);
        }
        return prompt;
    }
}
