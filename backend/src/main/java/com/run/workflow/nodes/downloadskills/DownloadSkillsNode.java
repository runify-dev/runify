package com.run.workflow.nodes.downloadskills;

import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Skill;
import com.run.dao.entity.SkillFile;
import com.run.dao.mapper.FileMapper;
import com.run.dao.mapper.SkillFileMapper;
import com.run.dao.mapper.SkillMapper;
import com.run.sql.DSL;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.downloadskills.entity.DownloadSkillsNodeData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class DownloadSkillsNode extends INode<DownloadSkillsNode, DownloadSkillsNodeData> {

    public final static String type = "download-skills-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public DownloadSkillsNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public DownloadSkillsNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record DownloadSkillsConfig(String id, String skillId, boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("skill_id", skillId));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, DownloadSkillsNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, DownloadSkillsNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            DownloadSkillsNodeData data = node.params;

            DownloadSkillsConfig config = resolveConfig(data, workFlowManage);
            if (config == null || StringUtils.isEmpty(config.skillId())) {
                return invokeFail(workFlowManage, node, config, runId, "技能ID为空");
            }

            if (config.withWriteArguments()) {
                workFlowManage.write(node, new ToolCallContent("download_skills", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
            }

            SkillMapper skillMapper = RunApplication.appComponent.skillMapper();
            SkillFileMapper skillFileMapper = RunApplication.appComponent.skillFileMapper();
            FileMapper fileMapper = RunApplication.appComponent.fileMapper();
            Vertx vertx = RunApplication.appComponent.vertx();

            UUID skillUuid;
            try {
                skillUuid = UUID.fromString(config.skillId());
            } catch (IllegalArgumentException e) {
                return invokeFail(workFlowManage, node, config, runId, "无效的技能ID: " + config.skillId());
            }

            // 查询技能信息
            skillMapper.getById(config.skillId())
                    .onSuccess(skill -> {
                        if (skill == null) {
                            invokeFail(workFlowManage, node, config, runId, "技能不存在: " + config.skillId());
                            return;
                        }

                        // 查询技能文件列表
                        skillFileMapper.list(DSL.field(SkillFile::getSkillId).eq(skillUuid))
                                .onSuccess(skillFiles -> {
                                    if (node.getStatus() == NodeStatus.CANCELLED) return;

                                    // 确定本地目录
                                    Path localDir = workFlowManage.getApplicationDirectory()
                                            .resolve("skills").resolve(skill.getName());

                                    // 下载所有文件
                                    downloadAll(vertx, fileMapper, localDir, skillFiles)
                                            .onSuccess(downloadedFiles -> {
                                                if (node.getStatus() == NodeStatus.CANCELLED) return;

                                                // 生成 .env 和 .skill-meta.json
                                                writeEnvFile(localDir, skill);
                                                writeMetaFile(localDir, skill);

                                                String summary = "已安装 " + skill.getName() + " (" + downloadedFiles.size() + " 个文件)";
                                                String resultJson = JacksonUtils.toJson(Map.of(
                                                        "skillId", skill.getId().toString(),
                                                        "skillName", skill.getName(),
                                                        "files", downloadedFiles.size(),
                                                        "localPath", localDir.toString()));

                                                workFlowManage.writeContext(node, "skillId", skill.getId().toString());
                                                workFlowManage.writeContext(node, "skillName", skill.getName());
                                                workFlowManage.writeContext(node, "files", downloadedFiles.size());
                                                workFlowManage.writeContext(node, "status", "installed");
                                                workFlowManage.writeContext(node, "localPath", localDir.toString());
                                                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(
                                                        new ToolCallContent("download_skills", resultJson, config.toArguments(),
                                                                NodeStatus.SUCCESS, node, runId, config.id()).withMeta(config.meta())));
                                                node.status = NodeStatus.SUCCESS;

                                                workFlowManage.write(node, new ToolCallContent("download_skills", summary, "",
                                                        NodeStatus.SUCCESS, node, runId, config.id()));
                                                workFlowManage.nextInvoke(node, workFlowManage.nextNodeSupplier(node.node.getId()));
                                            })
                                            .onFailure(e -> invokeFail(workFlowManage, node, config, runId, "下载失败: " + e.getMessage()));
                                })
                                .onFailure(e -> invokeFail(workFlowManage, node, config, runId, "查询文件列表失败: " + e.getMessage()));
                    })
                    .onFailure(e -> invokeFail(workFlowManage, node, config, runId, "查询技能失败: " + e.getMessage()));

            return null;
        }

        /**
         * 下载所有技能文件到本地目录
         */
        private Future<List<String>> downloadAll(Vertx vertx, FileMapper fileMapper,
                                                   Path localDir, List<SkillFile> skillFiles) {
            List<Future<Void>> futures = new ArrayList<>();
            List<String> downloaded = new ArrayList<>();

            for (SkillFile sf : skillFiles) {
                if ("folder".equals(sf.getType())) {
                    // 创建目录
                    try {
                        Files.createDirectories(localDir.resolve(sf.getName()));
                        downloaded.add(sf.getName() + "/");
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                } else if ("text".equals(sf.getType())) {
                    // 写入文本文件
                    try {
                        Path filePath = localDir.resolve(sf.getName());
                        if (filePath.getParent() != null) Files.createDirectories(filePath.getParent());
                        Files.writeString(filePath, sf.getContent() != null ? sf.getContent() : "", StandardCharsets.UTF_8);
                        downloaded.add(sf.getName());
                    } catch (Exception e) {
                        return Future.failedFuture(e);
                    }
                } else if ("file".equals(sf.getType()) && sf.getFileId() != null) {
                    // 下载二进制文件
                    futures.add(fileMapper.getById(sf.getFileId().toString())
                            .compose(entity -> {
                                if (entity == null) return Future.succeededFuture();
                                Path filePath = localDir.resolve(sf.getName());
                                if (filePath.getParent() != null) {
                                    try { Files.createDirectories(filePath.getParent()); } catch (Exception e) { return Future.failedFuture(e); }
                                }
                                return downloadFile(vertx, fileMapper, entity, filePath.toFile())
                                        .onSuccess(v -> downloaded.add(sf.getName()));
                            }));
                }
            }

            if (futures.isEmpty()) {
                return Future.succeededFuture(downloaded);
            }

            return Future.all(futures).map(v -> downloaded);
        }

        private Future<Void> downloadFile(Vertx vertx, FileMapper fileMapper,
                                           com.run.dao.entity.FileEntity entity, File targetFile) {
            io.vertx.core.Promise<Void> promise = io.vertx.core.Promise.promise();
            var readStream = fileMapper.downloadFile(vertx, entity);
            vertx.fileSystem().open(targetFile.getAbsolutePath(),
                    new io.vertx.core.file.OpenOptions().setWrite(true).setCreate(true))
                    .onSuccess(asyncFile -> {
                        readStream.handler(asyncFile::write);
                        readStream.endHandler(v -> { asyncFile.close(); promise.complete(); });
                        readStream.exceptionHandler(e -> { asyncFile.close(); promise.fail(e); });
                        readStream.read();
                    })
                    .onFailure(promise::fail);
            return promise.future();
        }

        /**
         * 写入 .skill-meta.json（记录下载时间用于更新检测）
         */
        private void writeMetaFile(Path localDir, Skill skill) {
            try {
                JsonObject meta = new JsonObject();
                meta.put("skillId", skill.getId().toString());
                meta.put("name", skill.getName());
                meta.put("downloadTime", java.time.LocalDateTime.now().toString());
                if (skill.getUpdateTime() != null) {
                    meta.put("skillUpdateTime", skill.getUpdateTime().toString());
                }
                Files.createDirectories(localDir);
                Files.writeString(localDir.resolve(".skill-meta.json"),
                        meta.encodePrettily(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        /**
         * 从 skillParameterForm + parameterValue 生成 .env 文件
         */
        private void writeEnvFile(Path localDir, Skill skill) {
            try {
                JsonObject params = skill.decrypt();
                if (params == null || params.isEmpty()) return;

                JsonArray form = skill.getSkillParameterForm();
                if (form == null || form.isEmpty()) return;

                StringBuilder env = new StringBuilder();
                for (int i = 0; i < form.size(); i++) {
                    JsonObject field = form.getJsonObject(i);
                    String key = field.getString("field");
                    if (key == null || key.isEmpty()) continue;
                    Object value = params.getValue(key);
                    if (value != null) {
                        env.append(key).append("=").append(value.toString()).append("\n");
                    }
                }

                if (env.length() > 0) {
                    Path envFile = localDir.resolve(".env");
                    Files.createDirectories(localDir);
                    Files.writeString(envFile, env.toString(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // .env 生成失败不影响整体安装
            }
        }

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, DownloadSkillsNode node,
                                                  DownloadSkillsConfig config, String runId, String error) {
            node.status = NodeStatus.FAIL;
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            String args = config != null ? config.toArguments() : "";
            wfm.writeContext(node, "tool", JsonObject.mapFrom(
                    new ToolCallContent("download_skills", error, args, NodeStatus.FAIL, node, runId, id)));
            wfm.nextFailInvoke(node, new RuntimeException(error));
            return null;
        }

        private DownloadSkillsConfig resolveConfig(DownloadSkillsNodeData data, WorkFlowManage wfm) {
            if ("tool_call".equals(data.getLocation())) {
                return resolveFromRef(data.getReference(), wfm);
            }
            String skillId = resolveValue(data.getSkillIdLocation(), data.getSkillIdReference(), data.getSkillId(), wfm);
            return new DownloadSkillsConfig(CommonUtils.uuid7().toString(), skillId, true, ToolCallMeta.EMPTY);
        }

        private DownloadSkillsConfig resolveFromRef(List<String> reference, WorkFlowManage wfm) {
            if (reference == null || reference.isEmpty()) return null;
            Object val = wfm.getContextVariable(reference);
            if (val instanceof JsonObject jo) {
                ToolCallMeta meta = ToolCallMeta.from(jo);
                String id = jo.getString("id");
                String args = jo.getString("functionArguments");
                if (args != null) {
                    JsonObject parsed = new JsonObject(args);
                    return new DownloadSkillsConfig(
                            StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                            parsed.getString("skill_id"), false, meta);
                }
                return new DownloadSkillsConfig(
                        StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        jo.getString("skillId"), false, meta);
            }
            return null;
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = wfm.getContextVariable(reference);
                    if (val instanceof JsonObject v) return v.getString("skillId");
                    if (val instanceof String v) return v;
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }
    }

    @Override
    public DownloadSkillsNodeData getNodeData(JsonObject params) {
        return node.getProperties().getJsonObject("nodeData").mapTo(DownloadSkillsNodeData.class);
    }

    @Override
    public NodeResult<DownloadSkillsNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
