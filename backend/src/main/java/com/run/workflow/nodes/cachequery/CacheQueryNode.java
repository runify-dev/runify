package com.run.workflow.nodes.cachequery;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.DatasourceMapper;
import com.run.datasources.DataSourceManage;
import com.run.datasources.SimpleCache;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.nodes.cachequery.pojo.CacheQueryNodeData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class CacheQueryNode extends INode<CacheQueryNode, CacheQueryNodeData> {

    public final static String type = "cache-query-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP);

    public CacheQueryNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public CacheQueryNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, CacheQueryNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, CacheQueryNode node) {
            CacheQueryNodeData data = node.params;
            DatasourceMapper mapper = RunApplication.appComponent.dataSourceMapper();
            Vertx vertx = RunApplication.appComponent.vertx();

            DataSourceManage.getCacheAsync(UUID.fromString(data.getCacheId()), (uuid, dm) -> dm.getById(uuid.toString()), mapper, vertx)
                    .onSuccess(cache -> {
                        String cacheKey = resolveKey(data, workFlowManage);
                        cache.get(cacheKey, Object.class)
                                .thenAccept(optional -> {
                                    Object result = optional.orElse(null);
                                    workFlowManage.writeContext(node, "result", result);
                                    node.status = NodeStatus.SUCCESS;
                                    workFlowManage.nextInvoke(node, () -> workFlowManage
                                            .getNextList(node.node.getId())
                                            .stream()
                                            .map(DefaultKeyValue::getValue)
                                            .toList());
                                })
                                .exceptionally(e -> {
                                    node.status = NodeStatus.FAIL;
                                    workFlowManage.write(node, new FailureContent(e.getMessage(), node,
                                            (String) workFlowManage.getParams().get("workflowRunId"),
                                            CommonUtils.uuid7().toString()));
                                    workFlowManage.end();
                                    return null;
                                });
                    })
                    .onFailure(e -> {
                        node.status = NodeStatus.FAIL;
                        workFlowManage.write(node, new FailureContent(e.getMessage(), node,
                                (String) workFlowManage.getParams().get("workflowRunId"),
                                CommonUtils.uuid7().toString()));
                        workFlowManage.end();
                    });
            return null;
        }

        private String resolveKey(CacheQueryNodeData data, WorkFlowManage workFlowManage) {
            String keyLocation = data.getKeyLocation();
            if (keyLocation == null) keyLocation = "customize";
            if (Strings.CS.equals(keyLocation, "reference")) {
                List<String> ref = data.getKeyReference();
                if (ref != null && !ref.isEmpty()) {
                    Object val = workFlowManage.getContextVariable(ref);
                    return val != null ? val.toString() : "";
                }
                return "";
            }
            return data.getKey() != null ? data.getKey() : "";
        }
    }

    @Override
    public CacheQueryNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(CacheQueryNodeData.class);
    }

    @Override
    public NodeResult<CacheQueryNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
