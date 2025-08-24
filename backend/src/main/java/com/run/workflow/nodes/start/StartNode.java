package com.run.workflow.nodes.start;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.openai.request.message.Message;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.start.entity.StartNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class StartNode extends INode<StartNode, StartNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "start-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

    public StartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public StartNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, StartNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, StartNode node) {
            node.write();
            workFlowManage.writeContext(node, "messages", node.params.messages);
            workFlowManage.writeContext(node, "question", node.params.messages.getLast().getContent());
            node.end(NodeStatus.SUCCESS);
            return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();

        }
    }


    @Override
    public StartNodeData getNodeData(JsonObject params) {
        List<Message> messages = (List<Message>) params.getMap().get("messages");
        StartNodeData startNodeData = new StartNodeData();
        startNodeData.setMessages(messages);
        return startNodeData;
    }

    @Override
    public NodeResult<StartNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }


    public void write() {

    }
}
