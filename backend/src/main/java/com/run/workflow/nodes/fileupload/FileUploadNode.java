package com.run.workflow.nodes.fileupload;

import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.FileMapper;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.JacksonUtils;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.fileupload.pojo.FileUploadNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FileUploadNode extends INode<FileUploadNode, FileUploadNodeData> {

    public final static String type = "file-upload-node";

    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.PROCESSOR_HTTP, WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP_LOOP);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public FileUploadNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public FileUploadNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        cancelled.set(true);
    }

    record FileUploadConfig(String id, String filePath, boolean withWriteArguments) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("path", filePath));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, FileUploadNode, Supplier<List<Node>>> {

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, FileUploadNode node, FileUploadConfig config, String runId, Throwable e) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("file_upload", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, id)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, FileUploadNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            FileUploadNodeData data = node.params;
            FileMapper fileMapper = RunApplication.appComponent.fileMapper();

            FileUploadConfig config = resolveConfig(data, workFlowManage);

            if (config == null || config.filePath() == null || config.filePath().isEmpty()) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件路径为空"));
            }
            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("file_upload", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
            }

            UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
            Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
            Path filePath = basePath.resolve(config.filePath()).normalize();
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件不存在: " + config.filePath()));
            }

            String fileName = (data.getFileName() != null && !data.getFileName().isEmpty())
                    ? data.getFileName()
                    : file.getName();

            fileMapper.upload(fileName, file.length(), null, null, file)
                    .onSuccess(entity -> {
                        if (node.cancelled.get()) return;
                        workFlowManage.writeContext(node, "url", "./api/storage/file/" + entity.getId().toString());
                        workFlowManage.writeContext(node, "fileId", entity.getId().toString());
                        workFlowManage.writeContext(node, "fileName", entity.getFileName());
                        workFlowManage.writeContext(node, "fileSize", entity.getSize());
                        node.status = NodeStatus.SUCCESS;

                        String resultJson = JacksonUtils.toJson(Map.of(
                                "fileId", entity.getId().toString(),
                                "fileName", entity.getFileName(),
                                "fileSize", entity.getSize(),
                                "url", "./api/storage/file/" + entity.getId()));
                        workFlowManage.write(node, new ToolCallContent("file_upload", resultJson, "",
                                NodeStatus.SUCCESS, node, runId, config.id()));
                        workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("file_upload", resultJson, config.toArguments(),
                                NodeStatus.SUCCESS, node, runId, config.id())));
                        workFlowManage.nextInvoke(node, workFlowManage.nextNodeSupplier(node.node.getId()));
                    })
                    .onFailure(e -> {
                        if (node.cancelled.get()) return;
                        invokeFail(workFlowManage, node, config, runId, e);
                        workFlowManage.nextFailInvoke(node, e);

                    });
            return null;
        }

        private FileUploadConfig resolveConfig(FileUploadNodeData data, WorkFlowManage wfm) {
            String location = data.getLocation();
            if (location == null) location = "customize";
            if ("tool_call".equals(location)) {
                return resolveFromRef(data.getReference(), wfm);
            }
            if ("reference".equals(location)) {
                return resolveFromRef(data.getPathReference(), wfm);
            }
            return new FileUploadConfig(CommonUtils.uuid7().toString(), data.getPath(), true);
        }

        private FileUploadConfig resolveFromRef(List<String> reference, WorkFlowManage wfm) {
            if (reference == null || reference.isEmpty()) return null;
            Object val = wfm.getContextVariable(reference);
            if (val instanceof JsonObject v) {
                String id = v.getString("id");
                String args = v.getString("functionArguments");
                if (args != null) {
                    return new FileUploadConfig(id, new JsonObject(args).getString("path"), false);
                }
                return new FileUploadConfig(id, v.getString("path"), false);
            }
            if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                JsonObject args = JacksonUtils.fromJson(call.getFunctionArguments(), JsonObject.class);
                return new FileUploadConfig(call.getId(), args.getString("path"), false);
            }
            if (val instanceof String v) {
                return new FileUploadConfig(CommonUtils.uuid7().toString(), v, true);
            }
            return null;
        }
    }

    @Override
    public FileUploadNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(FileUploadNodeData.class);
    }

    @Override
    public NodeResult<FileUploadNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
