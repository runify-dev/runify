package com.run.workflow.nodes.fileupload;

import com.run.RunApplication;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.dao.mapper.FileMapper;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.JacksonUtils;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.fileupload.pojo.FileUploadNodeData;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.Strings;

import java.io.File;
import java.util.List;
import java.util.Map;
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

    public static class Handle implements BiFunction<WorkFlowManage, FileUploadNode, Supplier<List<Node>>> {

        record Resolved(String id, String filePath, Boolean withWriteArguments) {
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, FileUploadNode node) {
            FileUploadNodeData data = node.params;
            FileMapper fileMapper = RunApplication.appComponent.fileMapper();

            Resolved resolved = resolveValue(data.getPathLocation(), data.getPathReference(), data.getPath(), workFlowManage);

            if (resolved == null || resolved.filePath() == null || resolved.filePath().isEmpty()) {
                node.status = NodeStatus.FAIL;
                workFlowManage.write(node, new FailureContent("文件路径为空", node,
                        (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
                workFlowManage.end();
                return null;
            }

            String filePath = resolved.filePath();
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                node.status = NodeStatus.FAIL;
                workFlowManage.write(node, new FailureContent("文件不存在: " + filePath, node,
                        (String) workFlowManage.getParams().get("workflowRunId"),
                        CommonUtils.uuid7().toString()));
                workFlowManage.end();
                return null;
            }

            String fileName = (data.getFileName() != null && !data.getFileName().isEmpty())
                    ? data.getFileName()
                    : file.getName();

            String id = resolved.id();
            fileMapper.upload(fileName, file.length(), null, null, file)
                    .onSuccess(entity -> {
                        if (node.cancelled.get()) return;
                        workFlowManage.writeContext(node, "url", "./api/storage/file/" + entity.getId().toString());
                        workFlowManage.writeContext(node, "fileId", entity.getId().toString());
                        workFlowManage.writeContext(node, "fileName", entity.getFileName());
                        workFlowManage.writeContext(node, "fileSize", entity.getSize());
                        node.status = NodeStatus.SUCCESS;
                        if (resolved.withWriteArguments()) {
                            workFlowManage.write(node, new ToolCallContent("FileUpload", "", JacksonUtils.toJson(Map.of("filePath", filePath)), NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                        }
                        workFlowManage.write(node, new ToolCallContent("fileUpload",
                                JacksonUtils.toJson(Map.of(
                                        "fileId", entity.getId().toString(),
                                        "fileName", entity.getFileName(),
                                        "fileSize", entity.getSize(),
                                        "url", "./api/storage/file/" + entity.getId()
                                )),
                                "",
                                NodeStatus.SUCCESS, node,
                                (String) workFlowManage.getParams().get("workflowRunId"), id));
                        workFlowManage.writeContext(node, "tool", JacksonUtils.toJson(new ToolCallContent("fileUpload",
                                JacksonUtils.toJson(Map.of(
                                        "fileId", entity.getId().toString(),
                                        "fileName", entity.getFileName(),
                                        "fileSize", entity.getSize(),
                                        "url", "./api/storage/file/" + entity.getId()
                                )),
                                JacksonUtils.toJson(Map.of("filePath", filePath)),
                                NodeStatus.SUCCESS, node,
                                (String) workFlowManage.getParams().get("workflowRunId"), id)));
                        workFlowManage.nextInvoke(node, () -> workFlowManage
                                .getNextList(node.node.getId())
                                .stream()
                                .map(DefaultKeyValue::getValue)
                                .toList());
                    })
                    .onFailure(e -> {
                        if (node.cancelled.get()) return;
                        node.status = NodeStatus.FAIL;
                        workFlowManage.write(node, new FailureContent(e.getMessage(), node,
                                (String) workFlowManage.getParams().get("workflowRunId"),
                                CommonUtils.uuid7().toString()));
                        workFlowManage.end();
                    });
            return null;
        }

        /**
         * 通用值解析：reference 从上下文取，customize 直接返回
         */
        private Resolved resolveValue(String location, List<String> reference, String customValue, WorkFlowManage workFlowManage) {
            if (location == null) location = "customize";
            if (Strings.CS.equals(location, "reference")) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = workFlowManage.getContextVariable(reference);
                    if (val instanceof JsonObject v) {
                        String id = v.getString("id");
                        JsonObject args = JacksonUtils.fromJson(v.getString("arguments"), JsonObject.class);
                        return new Resolved(id, args.getString("filePath"), Boolean.FALSE);
                    }
                    if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                        JsonObject args = JacksonUtils.fromJson(call.getFunctionArguments(), JsonObject.class);
                        return new Resolved(call.getId(), args.getString("filePath"), Boolean.FALSE);
                    }
                    if (val instanceof String v) {
                        return new Resolved(CommonUtils.uuid7().toString(), v, Boolean.TRUE);
                    }
                    return null;
                }
                return null;
            }
            return new Resolved(CommonUtils.uuid7().toString(), customValue, Boolean.TRUE);
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
