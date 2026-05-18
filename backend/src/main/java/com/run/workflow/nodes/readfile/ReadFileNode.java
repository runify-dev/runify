package com.run.workflow.nodes.readfile;

import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.readfile.entity.ReadFileNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ReadFileNode extends INode<ReadFileNode, ReadFileNodeData> {

    public final static String type = "read-file-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ReadFileNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ReadFileNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record ReadFileConfig(String chunkId, String filePath, int offset, int limit, boolean withWriteArguments) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("path", filePath, "offset", offset, "limit", limit));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, ReadFileNode, Supplier<List<Node>>> {

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, ReadFileNode node, ReadFileConfig config, String runId, Throwable e) {
            String chunkId = config != null ? config.chunkId() : CommonUtils.uuid7().toString();
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("read_file", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, chunkId)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ReadFileNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            ReadFileConfig config = resolveReadFileConfig(node, workFlowManage);

            if (config == null) {
                return invokeFail(workFlowManage, node, null, runId, new RuntimeException("配置解析失败"));
            }
            if (StringUtils.isEmpty(config.filePath())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件路径为空"));
            }

            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("read_file", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.chunkId()));
            }

            try {
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
                Path targetPath = basePath.resolve(config.filePath()).normalize();

                if (!Files.exists(targetPath)) {
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("read_file", "文件不存在: " + config.filePath(), config.toArguments(),
                            NodeStatus.SUCCESS, node, runId, config.chunkId())));
                    node.status = NodeStatus.SUCCESS;
                    workFlowManage.write(node, new ToolCallContent("read_file", "", "",
                            NodeStatus.SUCCESS, node, runId, config.chunkId()));
                    return null;
                }

                List<String> allLines;
                try {
                    allLines = Files.readAllLines(targetPath);
                } catch (AccessDeniedException e) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("权限不足: " + config.filePath()));
                }
                int totalLines = allLines.size();

                int start = Math.max(0, config.offset() - 1);
                int end = config.limit() > 0 ? Math.min(start + config.limit(), totalLines) : totalLines;
                List<String> readLines = allLines.subList(start, end);

                int maxLineNum = end;
                int width = String.valueOf(maxLineNum).length();

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < readLines.size(); i++) {
                    int lineNum = start + i + 1;
                    String line = String.format("%" + width + "d→%s", lineNum, readLines.get(i));
                    if (i > 0) sb.append('\n');
                    sb.append(line);
                    workFlowManage.write(node, new ToolCallContent("read_file", line + "\n", "",
                            NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return workFlowManage.nextCancelNodeSupplier();
                }

                String content = sb.toString();
                String rawContent = String.join("\n", readLines);

                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "rawContent", rawContent);
                workFlowManage.writeContext(node, "totalLines", totalLines);
                workFlowManage.writeContext(node, "lines", readLines.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("read_file", content, config.toArguments(),
                        NodeStatus.SUCCESS, node, runId, config.chunkId())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("read_file", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));

            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private ReadFileConfig resolveReadFileConfig(ReadFileNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(node.params.getReference());
                JsonObject toolCall = toJsonObject(ref);
                if (toolCall == null) return null;

                String id = toolCall.containsKey("id") ? toolCall.getString("id") : null;
                String args = toolCall.getString("functionArguments");
                if (StringUtils.isEmpty(args)) return null;

                JsonObject parsed = new JsonObject(args);
                String filePath = parsed.getString("path");
                int offset = parsed.getInteger("offset", 0);
                int limit = parsed.getInteger("limit", -1);
                boolean withWriteArguments = StringUtils.isEmpty(id);
                String chunkId = withWriteArguments ? CommonUtils.uuid7().toString() : id;

                return new ReadFileConfig(chunkId, filePath, offset, limit, withWriteArguments);
            }

            String filePath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            String offsetStr = resolveValue(node.params.getOffsetLocation(), node.params.getOffsetReference(),
                    node.params.getOffset() != null ? String.valueOf(node.params.getOffset()) : null, wfm);
            int offset = parseInt(offsetStr, 0);
            String limitStr = resolveValue(node.params.getLimitLocation(), node.params.getLimitReference(),
                    node.params.getLimit() != null ? String.valueOf(node.params.getLimit()) : null, wfm);
            int limit = parseInt(limitStr, -1);

            return new ReadFileConfig(CommonUtils.uuid7().toString(), filePath, offset, limit, true);
        }

        private JsonObject toJsonObject(Object ref) {
            if (ref instanceof JsonObject jo) return jo;
            if (ref instanceof Map<?, ?> map) {
                JsonObject jo = new JsonObject();
                map.forEach((k, v) -> jo.put(k.toString(), v));
                return jo;
            }
            return null;
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = wfm.getContextVariable(reference);
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }

        private int parseInt(String s, int defaultVal) {
            if (StringUtils.isEmpty(s)) return defaultVal;
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
    }

    @Override
    public ReadFileNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ReadFileNodeData data = new ReadFileNodeData();
        data.setLocation(jsonObject.getString("location"));

        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }

        data.setPathLocation(jsonObject.getString("pathLocation"));
        if (jsonObject.getJsonArray("pathReference") != null) {
            data.setPathReference(jsonObject.getJsonArray("pathReference").stream().map(Object::toString).toList());
        }
        data.setPath(jsonObject.getString("path"));

        data.setOffsetLocation(jsonObject.getString("offsetLocation"));
        if (jsonObject.getJsonArray("offsetReference") != null) {
            data.setOffsetReference(jsonObject.getJsonArray("offsetReference").stream().map(Object::toString).toList());
        }
        data.setOffset(jsonObject.getInteger("offset"));

        data.setLimitLocation(jsonObject.getString("limitLocation"));
        if (jsonObject.getJsonArray("limitReference") != null) {
            data.setLimitReference(jsonObject.getJsonArray("limitReference").stream().map(Object::toString).toList());
        }
        data.setLimit(jsonObject.getInteger("limit"));

        return data;
    }

    @Override
    public NodeResult<ReadFileNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
