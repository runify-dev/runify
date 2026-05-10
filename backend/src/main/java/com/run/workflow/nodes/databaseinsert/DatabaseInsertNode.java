package com.run.workflow.nodes.databaseinsert;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.DatasourceMapper;
import com.run.datasources.DataSourceManage;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.nodes.databaseinsert.pojo.DatabaseInsertNodeData;
import com.run.workflow.nodes.databasesearch.pojo.DatabaseSearchNodeData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
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

public class DatabaseInsertNode extends INode<DatabaseInsertNode, DatabaseInsertNodeData> {

    public final static String type = "database-insert-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    public DatabaseInsertNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public DatabaseInsertNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, DatabaseInsertNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, DatabaseInsertNode node) {
            DatabaseInsertNodeData data = node.params;
            String poolId = node.params.getPoolId();
            DatasourceMapper datasourceMapper = RunApplication.appComponent.dataSourceMapper();
            Vertx vertx = RunApplication.appComponent.vertx();
            Future<Pool> cacheAsync = DataSourceManage.getPoolAsync(UUID.fromString(poolId), (uuid, dm) -> dm.getById(uuid.toString()), datasourceMapper, vertx);
            cacheAsync.onSuccess(pool -> {
                HashMap<String, Object> params = new HashMap<>();
                if (data.getParameters() != null) {
                    for (DatabaseSearchNodeData.Parameter parameter : data.getParameters()) {
                        String location = parameter.getLocation();
                        if (Strings.CS.equals(location, "reference")) {
                            List<String> reference = (List<String>) parameter.getValue();
                            params.put(parameter.getField(), workFlowManage.getContextVariable(reference));
                        } else {
                            params.put(parameter.getField(), parameter.getValue());
                        }
                    }
                }

                // 解析SQL模板
                String sql;
                String location = data.getLocation();
                if (location == null) {
                    location = "customize";
                }
                if (Strings.CS.equals(location, "reference")) {
                    List<String> reference = data.getReference();
                    if (reference == null || reference.isEmpty()) {
                        sql = "";
                    } else {
                        Object refValue = workFlowManage.getContextVariable(reference);
                        sql = refValue != null ? refValue.toString() : "";
                    }
                } else {
                    sql = data.getTemplate();
                    if (sql == null) {
                        sql = "";
                    }
                }

                if (StringUtils.isEmpty(sql)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.write(node, new FailureContent("SQL语句为空", node,
                            (String) workFlowManage.getParams().get("workflowRunId"),
                            CommonUtils.uuid7().toString()));
                    workFlowManage.end();
                }

                SqlTemplate.forQuery(pool, sql)
                        .execute(params).onSuccess(ok -> {
                            int affectedRows = ok.rowCount();
                            workFlowManage.writeContext(node, "affectedRows", affectedRows);
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
    public DatabaseInsertNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(DatabaseInsertNodeData.class);
    }

    @Override
    public NodeResult<DatabaseInsertNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
