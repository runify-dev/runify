package com.run.workflow.nodes.notesearch;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchRequest;
import com.run.common.search.SearchResult;
import com.run.common.search.SearchDocument;
import com.run.common.util.CommonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.nodes.notesearch.pojo.NoteSearchNodeData;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class NoteSearchNode extends INode<NoteSearchNode, NoteSearchNodeData> {

    public final static String type = "note-search-node";

    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public NoteSearchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public NoteSearchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, NoteSearchNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, NoteSearchNode node) {
            NoteSearchNodeData data = node.params;

            String keyword = resolveValue(data.getKeywordLocation(), data.getKeywordReference(), data.getKeyword(), workFlowManage);
            if (keyword == null || keyword.isEmpty()) {
                node.status = NodeStatus.FAIL;
                workFlowManage.write(node, new FailureContent("检索文本为空", node,
                        (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
                workFlowManage.end();
                return null;
            }

            int pageNo = parseInt(resolveValue(data.getPageNoLocation(), data.getPageNoReference(),
                    data.getPageNo() != null ? String.valueOf(data.getPageNo()) : null, workFlowManage), 1);
            int pageSize = parseInt(resolveValue(data.getPageSizeLocation(), data.getPageSizeReference(),
                    data.getPageSize() != null ? String.valueOf(data.getPageSize()) : null, workFlowManage), 10);

            SearchRequest.Builder requestBuilder = SearchRequest.builder("note")
                    .keyword(keyword)
                    .keywordFields("title", "content")
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .sortByScoreDesc();

            if (data.getFolderIds() != null && !data.getFolderIds().isEmpty()) {
                requestBuilder.exactFilter("folderId", data.getFolderIds());
            }

            SearchRequest request = requestBuilder.build();

            SearchClient searchClient = RunApplication.appComponent.searchClient();
            Future<SearchResult<SearchDocument>> future = toVertxFuture(searchClient.search(request));

            future.onSuccess(result -> {
                List<Map<String, Object>> hits = result.getHits().stream().map(hit -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", hit.getId());
                    item.put("score", hit.getScore());
                    item.put("source", hit.getSource().getFields());
                    item.put("highlights", hit.getHighlights());
                    return item;
                }).toList();

                Map<String, Object> output = new HashMap<>();
                output.put("hits", hits);
                output.put("total", result.getTotal());
                output.put("pageNo", result.getPageNo());
                output.put("pageSize", result.getPageSize());

                float topScore = result.getHits().isEmpty() ? 0 : result.getHits().get(0).getScore();
                workFlowManage.writeContext(node, "result", output);
                workFlowManage.writeContext(node, "hits", hits);
                workFlowManage.writeContext(node, "total", result.getTotal());
                workFlowManage.writeContext(node, "topScore", topScore);
                node.status = NodeStatus.SUCCESS;
                workFlowManage.nextInvoke(node, () -> workFlowManage
                        .getNextList(node.node.getId())
                        .stream()
                        .map(DefaultKeyValue::getValue)
                        .toList());
            }).onFailure(e -> {
                workFlowManage.nextInvoke(node, node.handleFail(workFlowManage, e));
            });

            return null;
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage workFlowManage) {
            if (location == null) location = "customize";
            if (Strings.CS.equals(location, "reference")) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = workFlowManage.getContextVariable(reference);
                    if (val instanceof JsonObject v) {
                        return v.getString("keyword");
                    }
                    if (val instanceof String v) {
                        return v;
                    }
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }

        private int parseInt(String value, int defaultValue) {
            if (value == null || value.isEmpty()) return defaultValue;
            try {
                int v = Integer.parseInt(value.trim());
                return v > 0 ? v : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private <T> Future<T> toVertxFuture(java.util.concurrent.CompletionStage<T> stage) {
            Promise<T> promise = Promise.promise();
            stage.thenAccept(promise::complete).exceptionally(t -> {
                promise.fail(t);
                return null;
            });
            return promise.future();
        }
    }

    @Override
    public NoteSearchNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(NoteSearchNodeData.class);
    }

    @Override
    public NodeResult<NoteSearchNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
