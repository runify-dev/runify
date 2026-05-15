package com.run.workflow.nodes.apppatch;

import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffReader;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.apppatch.entity.ApplyPatchNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public record FileChange(String operation, String path, int linesAdded, int linesRemoved) {
    }

    public record Failure(String path, String reason, String suggestion) {
    }

    public record Result(boolean success, String summary, List<FileChange> applied, List<Failure> failures) {
    }

    private record StagedChange(String operation, Path target, List<String> newLines, int added, int removed) {
    }

    // ── 执行 ──
    record PatchConfig(String id, String patch, Path workDir, boolean withWriteArguments) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("patch", patch));
        }
    }

    /**
     * 读取文件行，优先 UTF-8，失败后 fallback 到系统默认编码。
     */
    private static List<String> readFileLines(Path target) throws IOException {
        try {
            return Files.readAllLines(target, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            return Files.readAllLines(target, Charset.defaultCharset());
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, ApplyPatchNode, Supplier<List<Node>>> {

        private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile(
                "@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*");

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, ApplyPatchNode node, PatchConfig config, String runId, Throwable e) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            String arguments = config != null ? config.toArguments() : "";
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("apply_patch", e.getMessage(), arguments,
                    NodeStatus.FAIL, node, runId, id)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ApplyPatchNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            PatchConfig config = resolvePatchConfig(node, workFlowManage);

            if (config == null || StringUtils.isEmpty(config.patch())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("patch 内容为空"));
            }

            try {
                String patchStr = fixHunkHeaders(normalizePatch(config.patch()));
                UnifiedDiff diff = UnifiedDiffReader.parseUnifiedDiff(
                        new ByteArrayInputStream(patchStr.getBytes(StandardCharsets.UTF_8)));

                if (diff.getFiles().isEmpty()) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("Patch 中没有可识别的文件改动"));
                }

                List<StagedChange> staged = new ArrayList<>();
                List<Failure> failures = new ArrayList<>();

                for (UnifiedDiffFile file : diff.getFiles()) {
                    if (node.getStatus() == NodeStatus.CANCELLED) return workFlowManage.nextCancelNodeSupplier();
                    try {
                        staged.add(stage(config.workDir(), file, patchStr));
                    } catch (Exception e) {
                        String path = file.getToFile() != null ? file.getToFile() : file.getFromFile();
                        failures.add(new Failure(stripPrefix(path), e.getMessage(), "请重新 read_file 确认文件内容后重新生成 patch"));
                    }
                }

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return workFlowManage.nextCancelNodeSupplier();
                }

                if (!failures.isEmpty()) {
                    String errMsg = failures.stream()
                            .map(f -> f.path() + ": " + f.reason())
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("应用失败（已回滚）:\n" + errMsg));
                }

                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("apply_patch", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
                }

                List<FileChange> applied = new ArrayList<>();
                for (StagedChange sc : staged) {
                    writeToDisk(sc);
                    String relPath = config.workDir().relativize(sc.target).toString();
                    applied.add(new FileChange(sc.operation, relPath, sc.added, sc.removed));
                    String verb = "created".equals(sc.operation) ? "Created" : "modified".equals(sc.operation) ? "Modified" : "Deleted";
                    workFlowManage.write(node, new ToolCallContent("apply_patch", verb + " " + relPath + "\n",
                            "", NodeStatus.RUNNING, node, runId, config.id()));
                }

                String summary = "成功应用 " + applied.size() + " 个文件的改动";
                Result result = new Result(true, summary, applied, List.of());
                writeResult(workFlowManage, node, runId, config, result);
                node.status = NodeStatus.SUCCESS;

            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private StagedChange stage(Path workDir, UnifiedDiffFile file, String patchStr) throws Exception {
            String fromPath = stripPrefix(file.getFromFile());
            String toPath = stripPrefix(file.getToFile());

            boolean isNew = "/dev/null".equals(file.getFromFile()) || fromPath == null;
            boolean isDelete = "/dev/null".equals(file.getToFile()) || toPath == null;

            // ── 新建文件 ──
            if (isNew) {
                Path target = resolve(workDir, toPath);
                if (Files.exists(target)) {
                    throw new IllegalStateException("文件已存在，不能用 new file 模式覆盖: " + toPath
                            + "。请使用修改模式（带上下文行的 diff）来编辑已有文件。");
                }
                List<String> content = extractNewFileContent(file, patchStr);
                return new StagedChange("created", target, content, content.size(), 0);
            }

            // ── 删除文件 ──
            if (isDelete) {
                Path target = resolve(workDir, fromPath);
                if (!Files.exists(target)) {
                    throw new IllegalStateException("文件不存在: " + fromPath);
                }
                int lines = readFileLines(target).size();
                return new StagedChange("deleted", target, null, 0, lines);
            }

            // ── 修改文件 ──
            Path target = resolve(workDir, toPath);
            if (!Files.exists(target)) {
                throw new IllegalStateException("文件不存在: " + toPath);
            }

            List<String> original = readFileLines(target);
            List<String> patched;
            try {
                patched = file.getPatch().applyTo(original);
            } catch (PatchFailedException e) {
                patched = applyPatchBySearchFallback(file.getPatch(), original, e);
            }

            if (patched.equals(original)) {
                throw new IllegalStateException("Patch 已解析，但没有产生任何文件变化。请检查 diff 的 -/+ 行是否真的不同。");
            }

            int added = 0;
            int removed = 0;
            for (AbstractDelta<String> delta : file.getPatch().getDeltas()) {
                removed += delta.getSource().size();
                added += delta.getTarget().size();
            }

            return new StagedChange("modified", target, patched, added, removed);
        }

        /**
         * java-diff-utils 默认会严格按 hunk header 的 position 应用。
         * AI 生成 patch 时最容易错的是 @@ -x,y +x,y @@ 的起始行号。
         *
         * 当 applyTo 失败时，这里按 source block 内容在文件中重新搜索最接近的位置，
         * 只要旧内容仍然存在，就可以降低“行号不准导致 patch 不生效”的概率。
         */
        private List<String> applyPatchBySearchFallback(
                Patch<String> patch,
                List<String> original,
                PatchFailedException cause
        ) {
            List<String> result = new ArrayList<>(original);

            List<AbstractDelta<String>> deltas = new ArrayList<>(patch.getDeltas());
            deltas.sort(Comparator.comparingInt(d -> d.getSource().getPosition()));

            int offset = 0;

            for (AbstractDelta<String> delta : deltas) {
                List<String> sourceLines = delta.getSource().getLines();
                List<String> targetLines = delta.getTarget().getLines();

                int preferred = delta.getSource().getPosition() + offset;
                preferred = Math.max(0, Math.min(preferred, result.size()));

                int pos;
                if (sourceLines.isEmpty()) {
                    // 纯插入没有旧内容可匹配，只能按修正后的位置插入。
                    pos = preferred;
                } else {
                    pos = findNearestBlock(result, sourceLines, preferred);
                    if (pos < 0) {
                        throw new IllegalStateException(buildPatchMismatchMessage(original, patch, cause));
                    }
                }

                result.subList(pos, pos + sourceLines.size()).clear();
                result.addAll(pos, targetLines);

                offset += targetLines.size() - sourceLines.size();
            }

            return result;
        }

        private int findNearestBlock(List<String> content, List<String> block, int preferred) {
            if (block.isEmpty()) {
                return Math.max(0, Math.min(preferred, content.size()));
            }

            int maxStart = content.size() - block.size();
            if (maxStart < 0) {
                return -1;
            }

            preferred = Math.max(0, Math.min(preferred, maxStart));

            int maxRadius = Math.max(preferred, maxStart - preferred);
            for (int radius = 0; radius <= maxRadius; radius++) {
                int left = preferred - radius;
                if (left >= 0 && matchesAt(content, block, left)) {
                    return left;
                }

                int right = preferred + radius;
                if (right != left && right <= maxStart && matchesAt(content, block, right)) {
                    return right;
                }
            }

            return -1;
        }

        private boolean matchesAt(List<String> content, List<String> block, int start) {
            if (start < 0 || start + block.size() > content.size()) {
                return false;
            }

            for (int i = 0; i < block.size(); i++) {
                if (!content.get(start + i).equals(block.get(i))) {
                    return false;
                }
            }

            return true;
        }

        private String buildPatchMismatchMessage(List<String> original, Patch<String> patch, PatchFailedException cause) {
            StringBuilder detail = new StringBuilder("上下文不匹配: ").append(cause.getMessage());

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                int pos = delta.getSource().getPosition();
                List<String> expected = delta.getSource().getLines();

                detail.append("\n\n--- patch 期望在第 ").append(pos + 1).append(" 行附近找到 ---");
                for (int k = 0; k < Math.min(8, expected.size()); k++) {
                    detail.append("\n  期望: ").append(expected.get(k));
                }

                if (pos < original.size()) {
                    detail.append("\n--- 文件实际第 ").append(pos + 1).append(" 行内容 ---");
                    for (int k = pos; k < Math.min(pos + 8, original.size()); k++) {
                        detail.append("\n  实际: ").append(original.get(k));
                    }
                } else {
                    detail.append("\n  实际: （超出文件范围，文件共 ").append(original.size()).append(" 行）");
                }
            }

            detail.append("\n\n提示: 请先用 read_file 重新读取文件内容，确保 diff 中的上下文行和 - 行与文件逐字一致。");
            return detail.toString();
        }

        private void writeToDisk(StagedChange sc) throws IOException {
            switch (sc.operation) {
                case "created" -> {
                    Files.createDirectories(sc.target.getParent());
                    Files.write(sc.target, sc.newLines, StandardCharsets.UTF_8);
                }
                case "modified" -> Files.write(sc.target, sc.newLines, StandardCharsets.UTF_8);
                case "deleted" -> Files.delete(sc.target);
                default -> throw new IllegalStateException("未知操作类型: " + sc.operation);
            }
        }

        private String stripPrefix(String path) {
            if (path == null) return null;
            if (path.startsWith("a/") || path.startsWith("b/")) return path.substring(2);
            return path;
        }

        /**
         * 归一化 AI 输出的 patch：
         * 1. CRLF/CR -> LF，避免 \r 进入行内容导致匹配失败；
         * 2. 兼容 ```diff 代码块；
         * 3. 补齐结尾换行。
         */
        private String normalizePatch(String patch) {
            if (patch == null) {
                return "";
            }

            String s = patch.replace("\r\n", "\n").replace("\r", "\n");

            String trimmed = s.stripLeading();
            if (trimmed.startsWith("```")) {
                int firstLineEnd = trimmed.indexOf('\n');
                if (firstLineEnd >= 0) {
                    trimmed = trimmed.substring(firstLineEnd + 1);
                }

                int lastFence = trimmed.lastIndexOf("\n```");
                if (lastFence >= 0 && trimmed.substring(lastFence).trim().equals("```")) {
                    trimmed = trimmed.substring(0, lastFence);
                }

                s = trimmed;
            }

            if (!s.endsWith("\n")) {
                s += "\n";
            }

            return s;
        }

        /**
         * 修正 hunk header 的行数部分，同时保留 AI 给的起始行号。
         *
         * 注意：这里只重算 count，不强行把 start 改成 1。
         * 如果 start 不准，后面的 applyPatchBySearchFallback 会按旧内容块兜底搜索。
         */
        private String fixHunkHeaders(String patch) {
            String[] rawLines = patch.split("\n", -1);

            int lineCount = rawLines.length;
            if (lineCount > 0 && rawLines[lineCount - 1].isEmpty()) {
                // split("\n", -1) 会把结尾换行变成最后一个空元素，这个不是 patch 的实际一行。
                lineCount--;
            }

            List<String> output = new ArrayList<>();

            for (int i = 0; i < lineCount; i++) {
                String line = rawLines[i];

                if (line.startsWith("@@")) {
                    Matcher m = HUNK_HEADER_PATTERN.matcher(line);

                    int origStart = 1;
                    int newStart = 1;

                    if (m.matches()) {
                        origStart = Integer.parseInt(m.group(1));
                        newStart = Integer.parseInt(m.group(2));
                    }

                    int oldCount = 0;
                    int newCount = 0;

                    int j = i + 1;
                    while (j < lineCount) {
                        String l = rawLines[j];

                        if (l.startsWith("@@") || l.startsWith("diff --git")) {
                            break;
                        }

                        if (l.startsWith("\\ No newline at end of file")) {
                            j++;
                            continue;
                        }

                        if (l.startsWith("-")) {
                            oldCount++;
                        } else if (l.startsWith("+")) {
                            newCount++;
                        } else {
                            // 上下文行：包括空格开头的行，也兼容完全空白的行。
                            oldCount++;
                            newCount++;
                        }

                        j++;
                    }

                    output.add("@@ -" + origStart + "," + oldCount + " +" + newStart + "," + newCount + " @@");
                } else {
                    output.add(line);
                }
            }

            return String.join("\n", output) + "\n";
        }

        /**
         * 从原始 patch 文本提取新建文件内容。
         *
         * 不使用 line.contains(path)，避免子串误匹配。
         */
        private List<String> extractNewFileContent(UnifiedDiffFile file, String patchStr) {
            String toFile = file.getToFile();
            String normalizedToFile = toFile.startsWith("b/") ? toFile.substring(2) : toFile;

            String exactMatchWithPrefix = "+++ b/" + normalizedToFile;
            String exactMatchDirect = "+++ " + toFile;

            List<String> content = new ArrayList<>();
            boolean foundFile = false;
            boolean inHunk = false;

            for (String line : patchStr.split("\n", -1)) {
                if (!foundFile && line.startsWith("+++ ")) {
                    String trimmed = line.trim();
                    if (trimmed.equals(exactMatchWithPrefix) || trimmed.equals(exactMatchDirect)) {
                        foundFile = true;
                        continue;
                    }
                }

                if (foundFile && line.startsWith("@@")) {
                    inHunk = true;
                    continue;
                }

                if (inHunk) {
                    if (line.startsWith("diff --git")) {
                        break;
                    }

                    if (line.startsWith("\\ No newline at end of file")) {
                        continue;
                    }

                    if (line.startsWith("+")) {
                        content.add(line.substring(1));
                    }
                }
            }

            if (content.isEmpty()) {
                file.getPatch().getDeltas().forEach(d -> content.addAll(d.getTarget().getLines()));
            }

            return content;
        }

        private Path resolve(Path workDir, String userPath) {
            if (StringUtils.isEmpty(userPath)) {
                throw new IllegalArgumentException("文件路径为空");
            }

            Path normalizedWorkDir = workDir.toAbsolutePath().normalize();
            Path resolved = normalizedWorkDir.resolve(userPath).toAbsolutePath().normalize();

            if (!resolved.startsWith(normalizedWorkDir)) {
                throw new SecurityException("路径越界: " + userPath);
            }

            return resolved;
        }

        private void writeResult(WorkFlowManage wfm, ApplyPatchNode node, String runId, PatchConfig config, Result result) {
            wfm.writeContext(node, "result", result.success());
            wfm.writeContext(node, "stdout", result.summary());
            wfm.writeContext(node, "stderr", result.failures().stream()
                    .map(f -> f.path() + ": " + f.reason())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b));
            NodeStatus status = result.success() ? NodeStatus.SUCCESS : NodeStatus.FAIL;
            wfm.write(node, new ToolCallContent("apply_patch", result.summary(), "", status, node, runId, config.id()));
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("apply_patch", result.summary(), config.toArguments(), status, node, runId, config.id())));
        }

        private PatchConfig resolvePatchConfig(ApplyPatchNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object val = wfm.getContextVariable(node.params.getReference());
                if (val == null) return null;

                String id = null;
                String args = null;
                if (val instanceof JsonObject jo) {
                    id = jo.getString("id");
                    args = jo.getString("functionArguments");
                }
                if (args == null) return null;

                JsonObject parsed = new JsonObject(args);
                String patchContent = parsed.getString("patch");
                String toolPath = parsed.getString("path");

                Path workDir;
                if (!StringUtils.isEmpty(toolPath)) {
                    UUID convId = (UUID) wfm.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                    Path base = Path.of(System.getProperty("user.home") + "/.runify/" + convId);
                    createDirectoriesIfNeeded(base);
                    workDir = Path.of(toolPath).isAbsolute() ? Path.of(toolPath) : base.resolve(toolPath).normalize();
                } else {
                    workDir = resolveWorkDir(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
                }

                return new PatchConfig(
                        StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        patchContent, workDir, false);
            }

            String patchContent = resolveValue(
                    node.params.getPatchLocation(),
                    node.params.getPatchReference(),
                    node.params.getPatch(),
                    "patch",
                    wfm
            );
            Path workDir = resolveWorkDir(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            return new PatchConfig(CommonUtils.uuid7().toString(), patchContent, workDir, true);
        }

        private Path resolveWorkDir(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            UUID conversationId = (UUID) wfm.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
            Path base = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
            createDirectoriesIfNeeded(base);

            String workDirStr = resolveValue(location, reference, customValue, "path", wfm);
            if (StringUtils.isEmpty(workDirStr)) return base;
            return Path.of(workDirStr).isAbsolute() ? Path.of(workDirStr) : base.resolve(workDirStr).normalize();
        }

        private void createDirectoriesIfNeeded(Path dir) {
            if (Files.exists(dir)) {
                return;
            }

            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                throw new RuntimeException("创建目录失败: " + dir, e);
            }
        }

        private String resolveValue(
                String location,
                List<String> reference,
                String customValue,
                String fieldName,
                WorkFlowManage wfm
        ) {
            if ("tool_call".equals(location)) {
                if (reference == null || reference.isEmpty()) return null;
                Object val = wfm.getContextVariable(reference);
                if (val == null) return null;

                String args = null;
                if (val instanceof JsonObject jo) {
                    args = jo.getString("functionArguments");
                }

                if (args == null) return val.toString();

                try {
                    return new JsonObject(args).getString(fieldName);
                } catch (Exception e) {
                    return args;
                }
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
        data.setPatchLocation(jsonObject.getString("patchLocation"));
        if (jsonObject.getJsonArray("patchReference") != null) {
            data.setPatchReference(jsonObject.getJsonArray("patchReference").stream().map(Object::toString).toList());
        }
        data.setPatch(jsonObject.getString("patch"));
        data.setPath(jsonObject.getString("path"));
        data.setPathLocation(jsonObject.getString("pathLocation"));
        if (jsonObject.getJsonArray("pathReference") != null) {
            data.setPathReference(jsonObject.getJsonArray("pathReference").stream().map(Object::toString).toList());
        }
        return data;
    }

    @Override
    public NodeResult<ApplyPatchNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
