package com.run.workflow.nodes.filedownload;

import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.filedownload.entity.FileDownloadNodeData;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FileDownloadNode extends INode<FileDownloadNode, FileDownloadNodeData> {

    public final static String type = "file-download-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    final AtomicBoolean cancelled = new AtomicBoolean(false);

    public FileDownloadNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public FileDownloadNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        cancelled.set(true);
    }

    record FileDownloadConfig(String id, String fileId, String path, boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            JsonObject entries = new JsonObject();
            entries.put("file_id", fileId);
            entries.put("path", path);
            return entries.toString();
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, FileDownloadNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, FileDownloadNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            FileDownloadNodeData data = node.params;

            FileDownloadConfig config = resolveConfig(data, workFlowManage);
            if (config == null || StringUtils.isEmpty(config.fileId())) {
                invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件ID为空"));
                return null;
            }
            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("file_download", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
            }

            FileMapper fileMapper = RunApplication.appComponent.fileMapper();
            Vertx vertx = RunApplication.appComponent.vertx();
            String fileId = extractFileId(config.fileId());
            fileMapper.getById(fileId)
                    .onSuccess(entity -> {
                        if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                        if (entity == null) {
                            invokeFail(workFlowManage, node, config, runId, new RuntimeException("文件不存在: " + config.fileId()));
                            return;
                        }

                        // 确定输出路径
                        Path basePath = workFlowManage.getWorkingDirectory();
                        String outputPath = StringUtils.isEmpty(config.path()) ? entity.getFileName() : config.path();
                        String decodedPath = java.net.URLDecoder.decode(outputPath, StandardCharsets.UTF_8);
                        Path targetPath = basePath.resolve(decodedPath);
                        java.io.File targetFile = targetPath.toFile();

                        // 确保目录存在
                        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                            targetFile.getParentFile().mkdirs();
                        }

                        // 下载文件
                        downloadToFile(vertx, fileMapper, entity, targetFile)
                                .onSuccess(v -> {
                                    if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }

                                    String resultJson = JacksonUtils.toJson(Map.of(
                                            "filePath", targetPath.toString(),
                                            "fileName", entity.getFileName(),
                                            "fileSize", entity.getSize()));
                                    writeDownloadsState(workFlowManage, config.fileId(), decodedPath, "");
                                    workFlowManage.write(node, new ToolCallContent("file_download",
                                            resultJson, "",
                                            NodeStatus.SUCCESS, node, runId, config.id()));
                                    workFlowManage.writeContext(node, "filePath", targetPath.toString());
                                    workFlowManage.writeContext(node, "fileName", entity.getFileName());
                                    workFlowManage.writeContext(node, "fileSize", entity.getSize());
                                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(
                                            new ToolCallContent("file_download", resultJson, config.toArguments(),
                                                    NodeStatus.SUCCESS, node, runId, config.id()).withMeta(config.meta)));
                                    node.status = NodeStatus.SUCCESS;
                                    workFlowManage.nextInvoke(node, workFlowManage.nextNodeSupplier(node.node.getId()));
                                })
                                .onFailure(e -> {
                                    if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                                    invokeFail(workFlowManage, node, config, runId, e);
                                });
                    })
                    .onFailure(e -> {
                        if (node.cancelled.get()) { workFlowManage.nextInvoke(node, workFlowManage.nextCancelNodeSupplier()); return; }
                        invokeFail(workFlowManage, node, config, runId, e);
                    });

            return null;
        }

        private Future<Void> downloadToFile(Vertx vertx, FileMapper fileMapper, FileEntity entity, java.io.File targetFile) {
            Promise<Void> promise = Promise.promise();
            BaseReadStream readStream = fileMapper.downloadFile(vertx, entity);

            vertx.fileSystem().open(targetFile.getAbsolutePath(), new OpenOptions().setWrite(true).setCreate(true))
                    .onSuccess(asyncFile -> {
                        readStream.handler(asyncFile::write);
                        readStream.endHandler(v -> {
                            asyncFile.close();
                            promise.complete();
                        });
                        readStream.exceptionHandler(e -> {
                            asyncFile.close();
                            promise.fail(e);
                        });
                        readStream.read();
                    })
                    .onFailure(promise::fail);

            return promise.future();
        }

        private void invokeFail(WorkFlowManage wfm, FileDownloadNode node, FileDownloadConfig config, String runId, Throwable e) {
            invokeFail(wfm, node, config, runId, e.getMessage());
        }

        private void invokeFail(WorkFlowManage wfm, FileDownloadNode node, FileDownloadConfig config, String runId, String error) {
            node.status = NodeStatus.FAIL;
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            String args = config != null ? config.toArguments() : "";
            writeDownloadsState(wfm, config != null ? config.fileId() : "", "", error);
            ToolCallMeta meta = config == null ? ToolCallMeta.EMPTY : config.meta;
            wfm.writeContext(node, "tool", JsonObject.mapFrom(
                    new ToolCallContent("file_download", error, args, NodeStatus.FAIL, node, runId, id).withMeta(meta)));
            wfm.nextFailInvoke(node, new RuntimeException(error));
        }

        private void writeDownloadsState(WorkFlowManage wfm, String fileId, String filePath, String error) {
            try {
                Path basePath = wfm.getWorkingDirectory();
                Path stateDir = basePath.resolve("_tool_state");
                Files.createDirectories(stateDir);
                Path stateFile = stateDir.resolve("downloads.json");

                List<Map<String, Object>> list;
                if (Files.exists(stateFile)) {
                    String existing = Files.readString(stateFile, StandardCharsets.UTF_8);
                    list = new ArrayList<>(JacksonUtils.fromJson(existing, new com.fasterxml.jackson.core.type.TypeReference<>() {
                    }));
                } else {
                    list = new ArrayList<>();
                }

                Map<String, Object> record = new LinkedHashMap<>();
                record.put("file_id", fileId);
                record.put("status", error.isEmpty() ? "success" : "fail");
                if (!filePath.isEmpty()) record.put("path", filePath);
                if (!error.isEmpty()) record.put("error", error);
                record.put("time", System.currentTimeMillis());
                list.add(record);

                Files.writeString(stateFile, JacksonUtils.toJson(list), StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {
            }
        }

        private FileDownloadConfig resolveConfig(FileDownloadNodeData data, WorkFlowManage wfm) {
            if ("tool_call".equals(data.getLocation())) {
                return resolveFromRef(data.getReference(), wfm);
            }

            String fileId = resolveValue(data.getFileIdLocation(), data.getFileIdReference(), data.getFileId(), wfm);
            String path = resolveValue(data.getPathLocation(), data.getPathReference(), data.getPath(), wfm);
            return new FileDownloadConfig(CommonUtils.uuid7().toString(), fileId, path, true, ToolCallMeta.EMPTY);
        }

        private FileDownloadConfig resolveFromRef(List<String> reference, WorkFlowManage wfm) {
            if (reference == null || reference.isEmpty()) return null;
            Object val = wfm.getContextVariable(reference);
            if (val instanceof JsonObject v) {
                String id = v.getString("id");
                String args = v.getString("functionArguments");
                ToolCallMeta meta = ToolCallMeta.from(v);
                if (args != null) {
                    JsonObject parsed = new JsonObject(args);
                    return new FileDownloadConfig(
                            StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                            parsed.getString("file_id"), parsed.getString("path"), false, meta);
                }
                return new FileDownloadConfig(
                        StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        v.getString("fileId"), v.getString("path"), false, meta);
            }
            return null;
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            if (location == null) location = "customize";
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = wfm.getContextVariable(reference);
                    if (val instanceof JsonObject v) return v.getString("fileId");
                    if (val instanceof String v) return v;
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }

        /**
         * 从 URL 路径中提取文件 ID
         * ./api/storage/file/de5d72e1-2cae-4b20-aceb-271627019f20 → de5d72e1-2cae-4b20-aceb-271627019f20
         * de5d72e1-2cae-4b20-aceb-271627019f20 → de5d72e1-2cae-4b20-aceb-271627019f20
         */
        private String extractFileId(String value) {
            if (StringUtils.isEmpty(value)) return value;
            int lastSlash = value.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < value.length() - 1) {
                return value.substring(lastSlash + 1);
            }
            return value;
        }
    }

    @Override
    public FileDownloadNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(FileDownloadNodeData.class);
    }

    @Override
    public NodeResult<FileDownloadNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
