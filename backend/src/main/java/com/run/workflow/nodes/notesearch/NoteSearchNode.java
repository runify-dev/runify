package com.run.workflow.nodes.notesearch;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchRequest;
import com.run.common.search.SearchResult;
import com.run.common.search.SearchDocument;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.notesearch.pojo.NoteSearchNodeData;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
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

    record SearchConfig(String id, String keyword, int pageNo, int pageSize, boolean withWriteArguments,
                        ToolCallMeta meta) {
    }

    public static class Handle implements BiFunction<WorkFlowManage, NoteSearchNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, NoteSearchNode node) {
            NoteSearchNodeData data = node.params;
            String runId = (String) workFlowManage.getParams().get("workflowRunId");

            SearchConfig config = resolveConfig(data, workFlowManage);
            if (config == null || StringUtils.isEmpty(config.keyword())) {
                invokeFail(workFlowManage, node, runId, config, new RuntimeException("检索文本为空"));
                return null;
            }

            String id = config.id();
            String keyword = config.keyword();

            SearchRequest.Builder requestBuilder = SearchRequest.builder("note")
                    .keyword(keyword)
                    .keywordFields("title", "content")
                    .pageNo(config.pageNo())
                    .pageSize(config.pageSize())
                    .sortByScoreDesc();

            if (data.getFolderIds() != null && !data.getFolderIds().isEmpty()) {
                requestBuilder.exactFilter("folderId", data.getFolderIds());
            }

            SearchRequest request = requestBuilder.build();

            // 写入参数（customize 模式）
            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("note_search", "",
                        JacksonUtils.toJson(Map.of("keyword", keyword, "page_no", config.pageNo(), "page_size", config.pageSize())),
                        NodeStatus.RUNNING, node, runId, id));
            }

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
                String summary = "找到 " + result.getTotal() + " 条结果";
                String argsJson = JacksonUtils.toJson(Map.of("keyword", keyword, "page_no", config.pageNo(), "page_size", config.pageSize()));

                workFlowManage.writeContext(node, "result", output);
                workFlowManage.writeContext(node, "hits", hits);
                workFlowManage.writeContext(node, "total", result.getTotal());
                workFlowManage.writeContext(node, "topScore", topScore);
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(
                        new ToolCallContent("note_search", summary, argsJson, NodeStatus.SUCCESS, node, runId, id).withMeta(config.meta)));
                node.status = NodeStatus.SUCCESS;
                workFlowManage.nextInvoke(node, () -> workFlowManage
                        .getNextList(node.node.getId())
                        .stream()
                        .map(DefaultKeyValue::getValue)
                        .toList());
            }).onFailure(e -> {
                invokeFail(workFlowManage, node, runId, config, e);
            });

            return null;
        }

        private SearchConfig resolveConfig(NoteSearchNodeData data, WorkFlowManage wfm) {
            if ("tool_call".equals(data.getLocation())) {
                if (data.getReference() == null || data.getReference().isEmpty()) return null;
                Object val = wfm.getContextVariable(data.getReference());
                if (val == null) return null;

                String id = null;
                String args = null;
                if (val instanceof JsonObject jo) {
                    id = jo.getString("id");
                    args = jo.getString("functionArguments");
                }
                if (args == null) return null;

                JsonObject parsed = new JsonObject(args);
                String keyword = parsed.getString("keyword");
                int pageNo = parsed.getInteger("page_no", 1);
                int pageSize = parsed.getInteger("page_size", 10);
                ToolCallMeta meta = ToolCallMeta.from(parsed);
                return new SearchConfig(
                        StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        keyword, pageNo, pageSize, false, meta);
            }

            // customize 模式
            String keyword = resolveValue(data.getKeywordLocation(), data.getKeywordReference(), data.getKeyword(), wfm);
            int pageNo = parseInt(resolveValue(data.getPageNoLocation(), data.getPageNoReference(),
                    data.getPageNo() != null ? String.valueOf(data.getPageNo()) : null, wfm), 1);
            int pageSize = parseInt(resolveValue(data.getPageSizeLocation(), data.getPageSizeReference(),
                    data.getPageSize() != null ? String.valueOf(data.getPageSize()) : null, wfm), 10);

            return new SearchConfig(CommonUtils.uuid7().toString(), keyword, pageNo, pageSize, true, ToolCallMeta.EMPTY);
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            if (location == null) location = "customize";
            if (Strings.CS.equals(location, "reference")) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = wfm.getContextVariable(reference);
                    if (val instanceof JsonObject v) return v.getString("keyword");
                    if (val instanceof String v) return v;
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

        private void invokeFail(WorkFlowManage wfm, NoteSearchNode node, String runId, SearchConfig config, Throwable e) {
            node.status = NodeStatus.FAIL;
            String id = config == null ? CommonUtils.uuid7().toString() : config.id;
            ToolCallMeta meta = config == null ? ToolCallMeta.EMPTY : config.meta;
            wfm.writeContext(node, "tool", JsonObject.mapFrom(
                    new ToolCallContent("note_search", e.getMessage(), "", NodeStatus.FAIL, node, runId, id).withMeta(meta)));
            wfm.nextFailInvoke(node, e);
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
