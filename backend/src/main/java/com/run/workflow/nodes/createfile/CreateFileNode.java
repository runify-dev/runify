package com.run.workflow.nodes.createfile;

import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.createfile.entity.CreateFileNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class CreateFileNode extends INode<CreateFileNode, CreateFileNodeData> {

    public final static String type = "create-file-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public CreateFileNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public CreateFileNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record CreateFileConfig(String chunkId, String filePath, String content,
                            boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("path", filePath, "content", content));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, CreateFileNode, Supplier<List<Node>>> {

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, CreateFileNode node, CreateFileConfig config, String runId, Throwable e) {
            node.status = NodeStatus.FAIL;
            String chunkId = config != null ? config.chunkId() : CommonUtils.uuid7().toString();
            ToolCallContent tc = new ToolCallContent("create_file", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, chunkId);
            if (config != null) tc.withMeta(config.meta());
            wfm.writeContext(node, "tool", JsonObject.mapFrom(tc));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, CreateFileNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            CreateFileConfig config = resolveConfig(node, workFlowManage);

            if (config == null) {
                return invokeFail(workFlowManage, node, null, runId, new RuntimeException("配置解析失败"));
            }
            if (StringUtils.isEmpty(config.filePath())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件路径为空"));
            }

            try {
                Path basePath = workFlowManage.getWorkingDirectory();
                Path target = basePath.resolve(config.filePath()).normalize();

                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("create_file", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                Path parent = target.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(target, config.content());

                String result = "文件创建成功: " + basePath.relativize(target);
                workFlowManage.writeContext(node, "result", result);
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("create_file", result, config.toArguments(),
                        NodeStatus.SUCCESS, node, runId, config.chunkId()).withMeta(config.meta())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("create_file", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));

            } catch (IOException e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private CreateFileConfig resolveConfig(CreateFileNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(node.params.getReference());
                JsonObject toolCall = toJsonObject(ref);
                if (toolCall == null) return null;

                String id = toolCall.getString("id");
                String args = toolCall.getString("functionArguments");
                if (StringUtils.isEmpty(args)) return null;

                JsonObject parsed = new JsonObject(args);
                String filePath = parsed.getString("path");
                String content = parsed.getString("content");
                boolean withWriteArguments = StringUtils.isEmpty(id);
                String chunkId = withWriteArguments ? CommonUtils.uuid7().toString() : id;
                ToolCallMeta meta = ToolCallMeta.from(toolCall);

                return new CreateFileConfig(chunkId, filePath, content, withWriteArguments, meta);
            }

            String filePath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            String content = resolveValue(node.params.getContentLocation(), node.params.getContentReference(), node.params.getContent(), wfm);

            return new CreateFileConfig(CommonUtils.uuid7().toString(), filePath, content, true, ToolCallMeta.EMPTY);
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
    }

    @Override
    public CreateFileNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        CreateFileNodeData data = new CreateFileNodeData();
        data.setLocation(jsonObject.getString("location"));

        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }

        data.setPathLocation(jsonObject.getString("pathLocation"));
        if (jsonObject.getJsonArray("pathReference") != null) {
            data.setPathReference(jsonObject.getJsonArray("pathReference").stream().map(Object::toString).toList());
        }
        data.setPath(jsonObject.getString("path"));

        data.setContentLocation(jsonObject.getString("contentLocation"));
        if (jsonObject.getJsonArray("contentReference") != null) {
            data.setContentReference(jsonObject.getJsonArray("contentReference").stream().map(Object::toString).toList());
        }
        data.setContent(jsonObject.getString("content"));

        return data;
    }

    @Override
    public NodeResult<CreateFileNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
