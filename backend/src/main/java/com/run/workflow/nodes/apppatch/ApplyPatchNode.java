package com.run.workflow.nodes.apppatch;

import com.github.difflib.patch.PatchFailedException;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffReader;
import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.apppatch.entity.ApplyPatchNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ApplyPatchNode extends INode<ApplyPatchNode, ApplyPatchNodeData> {

    public final static String type = "apply-patch-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ApplyPatchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ApplyPatchNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    // ── 结构体 ──

    public record FileChange(String operation, String path, int linesAdded, int linesRemoved) {}

    public record Failure(String path, String reason, String suggestion) {}

    public record Result(boolean success, String summary, List<FileChange> applied, List<Failure> failures) {}

    private record StagedChange(String operation, Path target, List<String> newLines, int added, int removed) {}

    // ── 执行 ──

    public static class Handle implements BiFunction<WorkFlowManage, ApplyPatchNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ApplyPatchNode node) {
            String patchStr = resolveValue(node.params.getLocation(), node.params.getReference(), node.params.getPatch(), workFlowManage);
            String runId = (String) workFlowManage.getParams().get("workflowRunId");

            if (StringUtils.isEmpty(patchStr)) {
                node.status = NodeStatus.FAIL;
                writeResult(workFlowManage, node, runId, patchStr,
                        new Result(false, "patch 内容为空", List.of(), List.of()));
                return next(workFlowManage, node);
            }

            Path workDir = Path.of(System.getProperty("user.dir"));

            try {
                // 解析 unified diff
                UnifiedDiff diff = UnifiedDiffReader.parseUnifiedDiff(
                        new ByteArrayInputStream(patchStr.getBytes(StandardCharsets.UTF_8)));

                if (diff.getFiles().isEmpty()) {
                    node.status = NodeStatus.FAIL;
                    writeResult(workFlowManage, node, runId, patchStr,
                            new Result(false, "Patch 中没有可识别的文件改动", List.of(), List.of()));
                    return next(workFlowManage, node);
                }

                // 干跑：先全部 stage，收集错误
                List<StagedChange> staged = new ArrayList<>();
                List<Failure> failures = new ArrayList<>();

                for (UnifiedDiffFile file : diff.getFiles()) {
                    try {
                        staged.add(stage(workDir, file));
                    } catch (Exception e) {
                        String path = file.getToFile() != null ? file.getToFile() : file.getFromFile();
                        failures.add(new Failure(stripPrefix(path), e.getMessage(), "请重新 read_file 确认文件内容"));
                    }
                }

                // 有失败则全部回滚
                if (!failures.isEmpty()) {
                    node.status = NodeStatus.FAIL;
                    String summary = "应用失败（已回滚）: " + failures.size() + " 个文件";
                    writeResult(workFlowManage, node, runId, patchStr,
                            new Result(false, summary, List.of(), failures));
                    return next(workFlowManage, node);
                }

                // 写盘
                List<FileChange> applied = new ArrayList<>();
                for (StagedChange sc : staged) {
                    writeToDisk(sc);
                    applied.add(new FileChange(sc.operation, workDir.relativize(sc.target).toString(), sc.added, sc.removed));
                }

                String summary = "成功应用 " + applied.size() + " 个文件的改动";
                Result result = new Result(true, summary, applied, List.of());
                writeResult(workFlowManage, node, runId, patchStr, result);
                node.status = NodeStatus.SUCCESS;

            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                writeResult(workFlowManage, node, runId, patchStr,
                        new Result(false, "解析 patch 失败: " + e.getMessage(), List.of(),
                                List.of(new Failure("", e.getMessage(), "检查 patch 格式是否正确"))));
            }

            return next(workFlowManage, node);
        }

        private StagedChange stage(Path workDir, UnifiedDiffFile file) throws Exception {
            String fromPath = stripPrefix(file.getFromFile());
            String toPath = stripPrefix(file.getToFile());

            boolean isNew = "/dev/null".equals(file.getFromFile()) || fromPath == null;
            boolean isDelete = "/dev/null".equals(file.getToFile()) || toPath == null;

            if (isNew) {
                Path target = resolve(workDir, toPath);
                List<String> content = new ArrayList<>();
                file.getPatch().getDeltas().forEach(d -> content.addAll(d.getTarget().getLines()));
                return new StagedChange("created", target, content, content.size(), 0);
            }

            if (isDelete) {
                Path target = resolve(workDir, fromPath);
                if (!Files.exists(target)) {
                    throw new IllegalStateException("文件不存在: " + fromPath);
                }
                int lines = Files.readAllLines(target).size();
                return new StagedChange("deleted", target, null, 0, lines);
            }

            Path target = resolve(workDir, toPath);
            if (!Files.exists(target)) {
                throw new IllegalStateException("文件不存在: " + toPath);
            }
            List<String> original = Files.readAllLines(target, StandardCharsets.UTF_8);
            try {
                List<String> patched = file.getPatch().applyTo(original);
                int added = patched.size() - original.size();
                int removed = 0;
                // 计算实际增删行数
                for (var delta : file.getPatch().getDeltas()) {
                    removed += delta.getSource().size();
                }
                return new StagedChange("modified", target, patched, Math.max(added, 0), removed);
            } catch (PatchFailedException e) {
                throw new IllegalStateException("上下文不匹配: " + e.getMessage());
            }
        }

        private void writeToDisk(StagedChange sc) throws IOException {
            switch (sc.operation) {
                case "created" -> {
                    Files.createDirectories(sc.target.getParent());
                    Files.write(sc.target, sc.newLines, StandardCharsets.UTF_8);
                }
                case "modified" -> Files.write(sc.target, sc.newLines, StandardCharsets.UTF_8);
                case "deleted" -> Files.delete(sc.target);
            }
        }

        private String stripPrefix(String path) {
            if (path == null) return null;
            if (path.startsWith("a/") || path.startsWith("b/")) return path.substring(2);
            return path;
        }

        private Path resolve(Path workDir, String userPath) {
            Path resolved = workDir.resolve(userPath).toAbsolutePath().normalize();
            if (!resolved.startsWith(workDir.toAbsolutePath().normalize())) {
                throw new SecurityException("路径越界: " + userPath);
            }
            return resolved;
        }

        private void writeResult(WorkFlowManage wfm, ApplyPatchNode node, String runId, String patchStr, Result result) {
            wfm.writeContext(node, "result", result.success());
            wfm.writeContext(node, "stdout", result.summary());
            wfm.writeContext(node, "stderr", result.failures().stream()
                    .map(f -> f.path() + ": " + f.reason())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b));
            wfm.write(node, new ToolCallContent("apply-patch", result.summary(),
                    JacksonUtils.toJson(Map.of("patch", patchStr)),
                    result.success() ? NodeStatus.SUCCESS : NodeStatus.FAIL,
                    node, runId, CommonUtils.uuid7().toString()));
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("apply-patch",
                    result.summary(), JacksonUtils.toJson(Map.of("patch", patchStr)),
                    result.success() ? NodeStatus.SUCCESS : NodeStatus.FAIL,
                    node, runId, CommonUtils.uuid7().toString())));
        }

        private Supplier<List<Node>> next(WorkFlowManage wfm, ApplyPatchNode node) {
            return () -> wfm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
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
    public ApplyPatchNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ApplyPatchNodeData data = new ApplyPatchNodeData();
        data.setLocation(jsonObject.getString("location"));
        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }
        data.setPatch(jsonObject.getString("patch"));
        return data;
    }

    @Override
    public NodeResult<ApplyPatchNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
