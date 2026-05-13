package com.run.workflow.nodes.readfile;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.readfile.entity.ReadFileNodeData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public static class Handle implements BiFunction<WorkFlowManage, ReadFileNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ReadFileNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");

            try {
                String filePath;
                int offset;
                int limit;

                if ("tool_call".equals(node.params.getLocation())) {
                    // tool_call 模式：从引用的 AccumulatedToolCall 中解析
                    if (node.params.getReference() == null || node.params.getReference().isEmpty()) {
                        node.status = NodeStatus.FAIL;
                        writeResult(workFlowManage, node, runId, "", "", 0, 0, "未指定引用变量");
                        return next(workFlowManage, node);
                    }
                    Object ref = workFlowManage.getContextVariable(node.params.getReference());
                    JsonObject toolCall = toJsonObject(ref);
                    if (toolCall == null) {
                        node.status = NodeStatus.FAIL;
                        writeResult(workFlowManage, node, runId, "", "", 0, 0, "引用变量格式错误");
                        return next(workFlowManage, node);
                    }
                    String args = toolCall.getString("functionArguments");
                    if (StringUtils.isEmpty(args)) {
                        node.status = NodeStatus.FAIL;
                        writeResult(workFlowManage, node, runId, "", "", 0, 0, "functionArguments 为空");
                        return next(workFlowManage, node);
                    }
                    JsonObject parsed = new JsonObject(args);
                    filePath = parsed.getString("path");
                    offset = parsed.getInteger("offset", 0);
                    limit = parsed.getInteger("limit", -1);
                } else {
                    // customize 模式
                    filePath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), workFlowManage);

                    String offsetStr = resolveValue(node.params.getOffsetLocation(), node.params.getOffsetReference(),
                            node.params.getOffset() != null ? String.valueOf(node.params.getOffset()) : null, workFlowManage);
                    offset = parseInt(offsetStr, 0);

                    String limitStr = resolveValue(node.params.getLimitLocation(), node.params.getLimitReference(),
                            node.params.getLimit() != null ? String.valueOf(node.params.getLimit()) : null, workFlowManage);
                    limit = parseInt(limitStr, -1);
                }

                if (StringUtils.isEmpty(filePath)) {
                    node.status = NodeStatus.FAIL;
                    writeResult(workFlowManage, node, runId, "", "", 0, 0, "文件路径为空");
                    return next(workFlowManage, node);
                }

                Path basePath = Path.of(System.getProperty("user.dir"));
                Path targetPath = basePath.resolve(filePath).normalize();

                if (!Files.exists(targetPath)) {
                    node.status = NodeStatus.FAIL;
                    writeResult(workFlowManage, node, runId, filePath, "", 0, 0, "文件不存在: " + filePath);
                    return next(workFlowManage, node);
                }

                List<String> allLines = Files.readAllLines(targetPath);
                int totalLines = allLines.size();

                int start = Math.max(0, offset);
                int end = limit > 0 ? Math.min(start + limit, totalLines) : totalLines;
                List<String> readLines = allLines.subList(start, end);

                // 原始内容（不带行号）
                String rawContent = String.join("\n", readLines);

                // 带行号输出：右对齐行号 + → + 内容
                int maxLineNum = end;
                int width = String.valueOf(maxLineNum).length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < readLines.size(); i++) {
                    if (i > 0) sb.append('\n');
                    int lineNum = start + i + 1;
                    sb.append(String.format("%" + width + "d→%s", lineNum, readLines.get(i)));
                }
                String content = sb.toString();

                String argsJson = JacksonUtils.toJson(Map.of("path", filePath, "offset", start, "limit", limit));
                ToolCallContent toolContent = new ToolCallContent("read_file", content, argsJson,
                        NodeStatus.SUCCESS, node, runId, CommonUtils.uuid7().toString());

                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "rawContent", rawContent);
                workFlowManage.writeContext(node, "totalLines", totalLines);
                workFlowManage.writeContext(node, "lines", readLines.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(toolContent));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, toolContent);

            } catch (IOException e) {
                node.status = NodeStatus.FAIL;
                writeResult(workFlowManage, node, runId, "", "", 0, 0, "IO 异常: " + e.getMessage());
            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                writeResult(workFlowManage, node, runId, "", "", 0, 0, "读取失败: " + e.getMessage());
            }

            return next(workFlowManage, node);
        }

        private Supplier<List<Node>> next(WorkFlowManage wfm, ReadFileNode node) {
            return () -> wfm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }

        private void writeResult(WorkFlowManage wfm, ReadFileNode node, String runId,
                                 String path, String content, int totalLines, int lines, String error) {
            wfm.writeContext(node, "content", content);
            wfm.writeContext(node, "totalLines", totalLines);
            wfm.writeContext(node, "lines", lines);
            if (error != null) {
                wfm.writeContext(node, "error", error);
                wfm.write(node, new ToolCallContent("read_file", error,
                        JacksonUtils.toJson(Map.of("path", path)),
                        NodeStatus.FAIL, node, runId, CommonUtils.uuid7().toString()));
            }
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
