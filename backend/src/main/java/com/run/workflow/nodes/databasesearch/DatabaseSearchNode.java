package com.run.workflow.nodes.databasesearch;


import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.DatasourceMapper;
import com.run.dao.mapper.ModelMapper;
import com.run.datasources.DataSourceManage;
import com.run.datasources.SimpleCache;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.nodes.databasesearch.pojo.DatabaseSearchNodeData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.templates.SqlTemplate;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
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
            UUID poolId = databaseSearchNodeData.getPoolId();
            DatasourceMapper datasourceMapper = RunApplication.appComponent.dataSourceMapper();
            Vertx vertx = RunApplication.appComponent.vertx();
            Future<Pool> cacheAsync = DataSourceManage.getPoolAsync(poolId, (uuid, dm) -> dm.getById(uuid.toString()), datasourceMapper, vertx);
            cacheAsync.onSuccess(pool -> {

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
                // 解析SQL模板
                String sql;
                String location = databaseSearchNodeData.getLocation();
                if (location == null) {
                    location = "customize";
                }
                if (Strings.CS.equals(location, "reference")) {
                    List<String> reference = databaseSearchNodeData.getReference();
                    if (reference == null || reference.isEmpty()) {
                        sql = "";
                    } else {
                        Object refValue = workFlowManage.getContextVariable(reference);
                        sql = refValue != null ? refValue.toString() : "";
                    }
                } else {
                    sql = databaseSearchNodeData.getTemplate();
                    if (sql == null) {
                        sql = "";
                    }
                }
                if (StringUtils.isEmpty(sql)) {
                    workFlowManage.nextFailInvoke(node, new RuntimeException("sql错误"));
                    return;
                }

                SqlTemplate.forQuery(pool, sql)
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
                            node.status = NodeStatus.FAIL;

                            workFlowManage.write(node, new FailureContent(e.getMessage(), node,
                                    (String) workFlowManage.getParams().get("workflowRunId"),
                                    CommonUtils.uuid7().toString()));
                            workFlowManage.end();
                        });

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
