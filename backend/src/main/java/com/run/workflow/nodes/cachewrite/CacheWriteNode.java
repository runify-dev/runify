package com.run.workflow.nodes.cachewrite;

import com.run.RunApplication;
import com.run.common.cache.CacheWriteOptions;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.DatasourceMapper;
import com.run.datasources.DataSourceManage;
import com.run.datasources.SimpleCache;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.nodes.cachewrite.pojo.CacheWriteNodeData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class CacheWriteNode extends INode<CacheWriteNode, CacheWriteNodeData> {

    public final static String type = "cache-write-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP, WorkflowType.PROCESSOR_HTTP_LOOP, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.CHAT_WORKFLOW);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public CacheWriteNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public CacheWriteNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        cancelled.set(true);
    }

    public static class Handle implements BiFunction<WorkFlowManage, CacheWriteNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, CacheWriteNode node) {
            CacheWriteNodeData data = node.params;
            DatasourceMapper mapper = RunApplication.appComponent.dataSourceMapper();
            Vertx vertx = RunApplication.appComponent.vertx();

            DataSourceManage.getCacheAsync(UUID.fromString(data.getCacheId()), (uuid, dm) -> dm.getById(uuid.toString()), mapper, vertx)
                    .onSuccess(cache -> {
                        if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                        String cacheKey = (String) resolveValue(data.getKeyLocation(), data.getKeyReference(), data.getKey(), workFlowManage);
                        Object cacheValue = resolveValue(data.getValueLocation(), data.getValueReference(), data.getValue(), workFlowManage);

                        CacheWriteOptions options = CacheWriteOptions.DEFAULT;
                        if (data.getTtl() != null && data.getTtl() > 0) {
                            options = CacheWriteOptions.ofTtl(Duration.ofSeconds(data.getTtl()));
                        }

                        cache.set(cacheKey, cacheValue, options)
                                .thenAccept(_ -> {
                                    if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                                    workFlowManage.writeContext(node, "success", true);
                                    node.status = NodeStatus.SUCCESS;
                                    workFlowManage.nextInvoke(node, () -> workFlowManage
                                            .getNextList(node.node.getId())
                                            .stream()
                                            .map(DefaultKeyValue::getValue)
                                            .toList());
                                })
                                .exceptionally(e -> {
                                    if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return null; }
                                    workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
                                    return null;
                                });
                    })
                    .onFailure(e -> {
                        if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                        workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
                    });
            return null;
        }

        private Object resolveValue(String location, List<String> reference, String customValue, WorkFlowManage workFlowManage) {
            if (location == null) location = "customize";
            if (Strings.CS.equals(location, "reference")) {
                if (reference != null && !reference.isEmpty()) {
                    return workFlowManage.getContextVariable(reference);
                }
                return "";
            }
            return customValue != null ? customValue : "";
        }
    }

    @Override
    public CacheWriteNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(CacheWriteNodeData.class);
    }

    @Override
    public NodeResult<CacheWriteNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
