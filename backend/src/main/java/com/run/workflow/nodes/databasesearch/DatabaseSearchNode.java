package com.run.workflow.nodes.databasesearch;


import com.run.common.keyvalue.DefaultKeyValue;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.nodes.databasesearch.pojo.DatabaseSearchNodeData;
import com.run.workflow.nodes.jsonresponse.pojo.JsonResponseNodeData;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.templates.SqlTemplate;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  21:50}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DatabaseSearchNode extends INode<DatabaseSearchNode, DatabaseSearchNodeData> {
    /**
     * 节点类型
     */
    public final static String type = "database-search-node";
    /**
     * 节点支持在什么工作流中运行
     */
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    @Override
    public List<Answer> getAnswerList(WorkFlowManage wm) {
        return List.of();
    }

    public DatabaseSearchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public DatabaseSearchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, DatabaseSearchNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, DatabaseSearchNode node) {
            DatabaseSearchNodeData databaseSearchNodeData = node.params;
            Pool pool = (Pool) workFlowManage.getContextVariable(databaseSearchNodeData.getPool());
            HashMap<String, Object> params = new HashMap<>();
            for (DatabaseSearchNodeData.Parameter parameter : databaseSearchNodeData.getParameters()) {
                String location = parameter.getLocation();
                if (Strings.CS.equals(location, "reference")) {
                    List<String> reference = (List<String>) parameter.getValue();
                    params.put(parameter.getField(), workFlowManage.getContextVariable(reference));
                } else {
                    params.put(parameter.getField(), parameter.getValue());
                }
            }
            SqlTemplate.forQuery(pool, databaseSearchNodeData.getTemplate())
                    .execute(params).onSuccess(ok -> {
                        List<Map<String, Object>> result = ok.stream().map(Row::toJson).map(JsonObject::getMap).toList();
                        workFlowManage.writeContext(node, "result", result);
                        node.status = NodeStatus.SUCCESS;
                        workFlowManage.nextInvoke(node, () -> workFlowManage
                                .getNextList(node.node.getId())
                                .stream()
                                .map(DefaultKeyValue::getValue)
                                .toList());
                    }).onFailure(e -> {
                     System.out.println(e);
                    });
            return null;
        }
    }


    @Override
    public DatabaseSearchNodeData getNodeData(JsonObject params) {
        JsonObject nodeParams = node.getProperties().getJsonObject("nodeData");
        return nodeParams.mapTo(DatabaseSearchNodeData.class);
    }

    @Override
    public NodeResult<DatabaseSearchNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }

}
