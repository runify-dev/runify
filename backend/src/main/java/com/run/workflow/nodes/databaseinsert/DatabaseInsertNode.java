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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class DatabaseInsertNode extends INode<DatabaseInsertNode, DatabaseInsertNodeData> {

    public final static String type = "database-insert-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public DatabaseInsertNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public DatabaseInsertNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        cancelled.set(true);
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
                if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
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
                    Supplier<List<Node>> listSupplier = node.handleFail(workFlowManage, new RuntimeException("SQL语句为空"));
                    workFlowManage.nextInvoke(node, listSupplier);
                    return;
                }

                SqlTemplate.forQuery(pool, sql)
                        .execute(params).onSuccess(ok -> {
                            if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                            int affectedRows = ok.rowCount();
                            workFlowManage.writeContext(node, "affectedRows", affectedRows);
                            node.status = NodeStatus.SUCCESS;
                            workFlowManage.nextInvoke(node, () -> workFlowManage
                                    .getNextList(node.node.getId())
                                    .stream()
                                    .map(DefaultKeyValue::getValue)
                                    .toList());
                        }).onFailure(e -> {
                            if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                            workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
                        });
            }).onFailure(e -> {
                workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
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
