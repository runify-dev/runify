package com.run.workflow.nodes.apppatch;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.github.difflib.unifieddiff.UnifiedDiff;
import com.github.difflib.unifieddiff.UnifiedDiffFile;
import com.github.difflib.unifieddiff.UnifiedDiffReader;
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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApplyPatchNode extends INode<ApplyPatchNode, ApplyPatchNodeData> {

    public final static String type = "apply-patch-node";
    public final static List<WorkflowType> supportWorkflow = List.of(WorkflowType.CHAT_WORKFLOW, WorkflowType.CHAT_WORKFLOW_LOOP, WorkflowType.PROCESSOR_HTTP, WorkflowType.PROCESSOR_HTTP_LOOP);

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

    /**
     * [FIX-2] originalBytes 保存改动前的原始字节，用于写盘失败时回滚（created 为 null）。
     * [FIX-3] lineSep / trailingNewline 用于按原文件的行尾风格和结尾换行状态写回，
     * 避免在 Windows 上把 LF 改成 CRLF，或给无尾换行的文件强加换行。
     * [FIX-7] renameFrom 非 null 表示这是一次重命名，target 为新路径，renameFrom 为旧路径。
     */
    private record StagedChange(String operation, Path target, Path renameFrom, List<String> newLines, String lineSep,
                                boolean trailingNewline, byte[] originalBytes, int added, int removed) {
    }

    /**
     * 解码后的文件内容，保留行尾风格与结尾换行信息。
     */
    private record FileContent(List<String> lines, String lineSep, boolean trailingNewline) {
    }

    /**
     * 新建文件提取出的内容，含是否以换行结尾。
     */
    private record NewFileContent(List<String> lines, boolean trailingNewline) {
    }

    // ── 执行 ──
    record PatchConfig(String id, String patch, Path workDir, boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("patch", patch));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, ApplyPatchNode, Supplier<List<Node>>> {

        private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile("@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*");

        /**
         * [FIX-5] 搜索兜底时，若同一旧内容块在文件中出现多处且块太短，
         * 视为歧义、拒绝应用，避免把改动悄悄贴到错误位置。
         */
        private static final int MIN_UNIQUE_BLOCK_LINES = 3;

        /**
         * [FIX-9] 零宽 / 不可见格式字符：零宽空格、零宽连接符、字间连接符、
         * 零宽不换行空格(BOM)、软连字符。这些字符肉眼不可见，Java 不当作空白，
         * NFC/NFKC 也不会删除，是导致"期望与实际打印一样却匹配失败"的常见元凶。
         */
        private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B\\u200C\\u200D\\u2060\\uFEFF\\u00AD]");

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, ApplyPatchNode node, PatchConfig config, String runId, Throwable e) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            String arguments = config != null ? config.toArguments() : "";
            ToolCallMeta meta = config == null ? ToolCallMeta.EMPTY : config.meta;
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();

            // [FIX-8] 失败路径也写齐下游上下文变量，避免下游读到 null 或上一轮旧值。
            wfm.writeContext(node, "result", false);
            wfm.writeContext(node, "stdout", "");
            wfm.writeContext(node, "stderr", msg);
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("apply_patch", msg, arguments, NodeStatus.FAIL, node, runId, id).withMeta(meta)));
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
                UnifiedDiff diff = UnifiedDiffReader.parseUnifiedDiff(new ByteArrayInputStream(patchStr.getBytes(StandardCharsets.UTF_8)));

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
                    String errMsg = failures.stream().map(f -> f.path() + ": " + f.reason()).reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("应用失败（未写入任何文件）:\n" + errMsg));
                }

                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("apply_patch", "", config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
                }

                // [FIX-2] 写盘阶段做真正的回滚：记录已成功写入的项，任一项失败时按原始字节逆序恢复。
                List<FileChange> applied = new ArrayList<>();
                List<StagedChange> done = new ArrayList<>();
                try {
                    for (StagedChange sc : staged) {
                        writeToDisk(sc);
                        done.add(sc);
                        String relPath = relativize(config.workDir(), sc.target());
                        applied.add(new FileChange(sc.operation(), relPath, sc.added(), sc.removed()));
                        String verb = switch (sc.operation()) {
                            case "created" -> "Created";
                            case "deleted" -> "Deleted";
                            case "renamed" -> "Renamed";
                            default -> "Modified";
                        };
                        workFlowManage.write(node, new ToolCallContent("apply_patch", verb + " " + relPath + "\n", "", NodeStatus.RUNNING, node, runId, config.id()));
                    }
                } catch (IOException io) {
                    rollback(done);
                    workFlowManage.write(node, new ToolCallContent("apply_patch", "写入失败，已回滚已应用的改动\n", "", NodeStatus.RUNNING, node, runId, config.id()));
                    throw new RuntimeException("应用失败（已回滚）: " + io.getMessage(), io);
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

        private String relativize(Path workDir, Path target) {
            try {
                return workDir.relativize(target).toString();
            } catch (Exception e) {
                return target.toString();
            }
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
                    throw new IllegalStateException("文件已存在，不能用 new file 模式覆盖: " + toPath + "。请使用修改模式（带上下文行的 diff）来编辑已有文件。");
                }
                NewFileContent nf = extractNewFileContent(file, patchStr);
                return new StagedChange("created", target, null, nf.lines(), "\n", nf.trailingNewline(), null, nf.lines().size(), 0);
            }

            // ── 删除文件 ──
            if (isDelete) {
                Path target = resolve(workDir, fromPath);
                if (!Files.exists(target)) {
                    throw new IllegalStateException("文件不存在: " + fromPath);
                }
                byte[] raw = Files.readAllBytes(target);
                int lines = decodeContent(raw).lines().size();
                return new StagedChange("deleted", target, null, null, "\n", false, raw, 0, lines);
            }

            // [FIX-7] 重命名：from / to 都存在且不同。
            boolean isRename = !fromPath.equals(toPath);

            Path readTarget = resolve(workDir, isRename ? fromPath : toPath);
            if (!Files.exists(readTarget)) {
                throw new IllegalStateException("文件不存在: " + (isRename ? fromPath : toPath));
            }

            byte[] raw = Files.readAllBytes(readTarget);
            FileContent fc = decodeContent(raw);
            List<String> original = fc.lines();

            List<String> patched;
            if (file.getPatch().getDeltas().isEmpty()) {
                // 纯重命名，无内容改动。
                patched = original;
            } else {
                try {
                    patched = file.getPatch().applyTo(original);
                } catch (PatchFailedException | IndexOutOfBoundsException e) {
                    // [FIX-10] hunk 起始行号越界时 applyTo 抛的是 IndexOutOfBoundsException 而非
                    // PatchFailedException，同样交给按内容搜索的兜底，而不是直接失败。
                    patched = applyPatchBySearchFallback(file.getPatch(), original, e);
                }
            }

            if (!isRename && patched.equals(original)) {
                throw new IllegalStateException("Patch 已解析，但没有产生任何文件变化。请检查 diff 的 -/+ 行是否真的不同。");
            }

            // [FIX-(count)] 基于实际改动重新计算增删行数，happy path 与兜底搜索都准确。
            int added = 0;
            int removed = 0;
            for (AbstractDelta<String> delta : DiffUtils.diff(original, patched).getDeltas()) {
                removed += delta.getSource().size();
                added += delta.getTarget().size();
            }

            if (isRename) {
                Path newTarget = resolve(workDir, toPath);
                if (Files.exists(newTarget)) {
                    throw new IllegalStateException("重命名目标已存在: " + toPath);
                }
                return new StagedChange("renamed", newTarget, readTarget, patched, fc.lineSep(), fc.trailingNewline(), raw, added, removed);
            }

            return new StagedChange("modified", readTarget, null, patched, fc.lineSep(), fc.trailingNewline(), raw, added, removed);
        }

        /**
         * java-diff-utils 默认会严格按 hunk header 的 position 应用。
         * AI 生成 patch 时最容易错的是 @@ -x,y +x,y @@ 的起始行号。
         * <p>
         * 当 applyTo 失败时，这里按 source block 内容在文件中重新搜索最接近的位置，
         * 只要旧内容仍然存在，就可以降低"行号不准导致 patch 不生效"的概率。
         * [FIX-5] 命中多处且块太短时判为歧义并拒绝，避免静默贴错位置。
         * <p>
         * [FIX-9] 注意：UnifiedDiffReader 会把整段 hunk 解析成一个 CHANGE delta，
         * source = 全部上下文行(含未改动行)。因此即便是纯新增 hunk，applyTo 也会逐字校验
         * 周围上下文；只要某行有一个不可见字符差异(全半角标点 / 破折号 / NBSP / 零宽 / BOM)，
         * applyTo 与本兜底都会失败。matchesAt 的 Level 5 用来吸收这类差异。
         */
        private List<String> applyPatchBySearchFallback(Patch<String> patch, List<String> original, Exception cause) {
            List<String> result = new ArrayList<>(original);

            List<AbstractDelta<String>> deltas = new ArrayList<>(patch.getDeltas());
            deltas.sort((a, b) -> Integer.compare(a.getSource().getPosition(), b.getSource().getPosition()));

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
                    if (pos == -2) {
                        throw new IllegalStateException("上下文不唯一，无法安全定位改动位置（旧内容在文件中出现多处且上下文过短）。" + "请在 diff 中提供更多上下文行后重试。\n" + buildPatchMismatchMessage(original, patch, cause));
                    }
                    if (pos < 0) {
                        throw new IllegalStateException(buildPatchMismatchMessage(original, patch, cause));
                    }
                }

                // [FIX-3] 把搜索偏差（drift）也累加进 offset，
                // 避免后续 delta 的 preferred 位置因前面的跳跃而错位。
                int drift = pos - preferred;

                result.subList(pos, pos + sourceLines.size()).clear();
                result.addAll(pos, targetLines);

                offset += drift + (targetLines.size() - sourceLines.size());
            }

            return result;
        }

        /**
         * 返回旧内容块在文件中的应用位置：
         * <ul>
         *     <li>>= 0：唯一或可信的最近匹配位置</li>
         *     <li>-1：未找到</li>
         *     <li>-2：匹配多处且块太短，歧义</li>
         * </ul>
         */
        private int findNearestBlock(List<String> content, List<String> block, int preferred) {
            if (block.isEmpty()) {
                return Math.max(0, Math.min(preferred, content.size()));
            }

            int maxStart = content.size() - block.size();
            if (maxStart < 0) {
                return -1;
            }

            preferred = Math.max(0, Math.min(preferred, maxStart));

            List<Integer> matches = new ArrayList<>();
            for (int i = 0; i <= maxStart; i++) {
                if (matchesAt(content, block, i)) {
                    matches.add(i);
                }
            }

            if (matches.isEmpty()) {
                return -1;
            }
            if (matches.size() == 1) {
                return matches.get(0);
            }

            // [FIX-5] 多处匹配：只有当块足够独特（行数较多）时才接受最近匹配，否则判为歧义。
            if (block.size() < MIN_UNIQUE_BLOCK_LINES) {
                return -2;
            }

            int best = matches.get(0);
            int bestDist = Math.abs(best - preferred);
            for (int m : matches) {
                int d = Math.abs(m - preferred);
                if (d < bestDist) {
                    best = m;
                    bestDist = d;
                }
            }
            return best;
        }

        /**
         * [FIX-5] 先精确匹配，再尝试 stripTrailing 容错匹配。
         * [FIX-6] 然后尝试 NFC 归一化匹配，处理 Unicode 字符表示不一致。
         * AI 生成的上下文行经常丢失尾部空格、使用不同的 Unicode 表示形式，
         * 或者 Tab/空格缩进不一致。
         * [FIX-9] 最后一级用 fuzzyMatchKey：NFKC（折叠全半角、NBSP/U+3000 等兼容差异）
         * + 删零宽/BOM + 折叠破折号变体，吸收"打印相同但字节不同"的最后一类差异。
         * 注意各级匹配只用于"定位"，不改写文件本身的字符表示。
         */
        private boolean matchesAt(List<String> content, List<String> block, int start) {
            if (start < 0 || start + block.size() > content.size()) {
                return false;
            }

            for (int i = 0; i < block.size(); i++) {
                String contentLine = content.get(start + i);
                String blockLine = block.get(i);
                // Level 1: 精确匹配
                if (contentLine.equals(blockLine)) {
                    continue;
                }
                // Level 2: stripTrailing 容错
                if (contentLine.stripTrailing().equals(blockLine.stripTrailing())) {
                    continue;
                }
                // Level 3: NFC 归一化 + stripTrailing
                String normContent = Normalizer.normalize(contentLine.stripTrailing(), Normalizer.Form.NFC);
                String normBlock = Normalizer.normalize(blockLine.stripTrailing(), Normalizer.Form.NFC);
                if (normContent.equals(normBlock)) {
                    continue;
                }
                // Level 4: [FIX-7] Tab/空格归一化（Tab→4空格，去尾部空白）
                if (normalizeWhitespace(normContent).equals(normalizeWhitespace(normBlock))) {
                    continue;
                }
                // Level 5: [FIX-9] NFKC + 删零宽/BOM + 折叠破折号，
                // 救全半角标点、破折号变体、NBSP、零宽字符、BOM 等不可见差异。
                if (fuzzyMatchKey(contentLine).equals(fuzzyMatchKey(blockLine))) {
                    continue;
                }
                return false;
            }

            return true;
        }

        /**
         * 将 Tab 替换为 4 空格，去除尾部空白。仅用于 matchesAt 的容错匹配，不改变实际文件内容。
         */
        private String normalizeWhitespace(String line) {
            return line.replace("\t", "    ").stripTrailing();
        }

        /**
         * [FIX-9] matchesAt 最宽松一级的比较 key（保留前导缩进，对代码安全）：
         * 1. 删除零宽 / BOM / 软连字符；
         * 2. NFKC 归一化（折叠全角↔半角标点、把 NBSP/U+3000 归一为普通空格、上下标/合字等）；
         * 3. 折叠常见破折号变体（U+2010–U+2015、U+2212）为半角连字符 '-'；
         * 4. 仅去尾部空白（不折叠行内空白，避免误伤代码缩进差异）。
         * 仅用于"定位"匹配，不会改写文件内容。
         */
        private static String fuzzyMatchKey(String line) {
            String t = ZERO_WIDTH.matcher(line).replaceAll("");
            t = Normalizer.normalize(t, Normalizer.Form.NFKC);
            t = t.replaceAll("[\\u2010-\\u2015\\u2212]", "-");
            return t.stripTrailing();
        }

        /**
         * [FIX-9] 把一行展开成码点序列，用于在错误信息中暴露不可见字符差异。
         */
        private static String toCodePoints(String s) {
            return s.codePoints()
                    .mapToObj(c -> String.format("U+%04X", c))
                    .collect(Collectors.joining(" "));
        }

        private String buildPatchMismatchMessage(List<String> original, Patch<String> patch, Exception cause) {
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

                // [FIX-9] 当期望行与实际行"看起来一样却逐字节不等"时，dump 第一处差异行的码点，
                // 直接暴露全半角标点 / 破折号 / NBSP / 零宽 / BOM 这类不可见字符。
                for (int k = 0; k < expected.size() && pos + k < original.size(); k++) {
                    String exp = expected.get(k);
                    String act = original.get(pos + k);
                    if (!exp.equals(act)) {
                        detail.append("\n  ⚠ 第 ").append(pos + k + 1).append(" 行逐字节不一致（码点）:");
                        detail.append("\n      期望: ").append(toCodePoints(exp));
                        detail.append("\n      实际: ").append(toCodePoints(act));
                        break; // 只报第一处，足够定位
                    }
                }
            }

            detail.append("\n\n提示: 请先用 read_file 重新读取文件内容，确保 diff 中的上下文行和 - 行与文件逐字一致。");
            return detail.toString();
        }

        private void writeToDisk(StagedChange sc) throws IOException {
            switch (sc.operation()) {
                case "created" -> {
                    Path parent = sc.target().getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    writeLines(sc.target(), sc.newLines(), sc.lineSep(), sc.trailingNewline());
                }
                case "modified" -> writeLines(sc.target(), sc.newLines(), sc.lineSep(), sc.trailingNewline());
                case "renamed" -> {
                    Path parent = sc.target().getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    writeLines(sc.target(), sc.newLines(), sc.lineSep(), sc.trailingNewline());
                    Files.delete(sc.renameFrom());
                }
                case "deleted" -> Files.delete(sc.target());
                default -> throw new IllegalStateException("未知操作类型: " + sc.operation());
            }
        }

        /**
         * [FIX-2] 逆序回滚已写入的改动。尽力恢复，单项恢复失败不影响其它项。
         */
        private void rollback(List<StagedChange> done) {
            for (int i = done.size() - 1; i >= 0; i--) {
                StagedChange sc = done.get(i);
                try {
                    switch (sc.operation()) {
                        case "created" -> Files.deleteIfExists(sc.target());
                        case "modified", "deleted" -> {
                            if (sc.originalBytes() != null) {
                                Files.write(sc.target(), sc.originalBytes());
                            }
                        }
                        case "renamed" -> {
                            Files.deleteIfExists(sc.target());
                            if (sc.renameFrom() != null && sc.originalBytes() != null) {
                                Files.write(sc.renameFrom(), sc.originalBytes());
                            }
                        }
                        default -> {
                        }
                    }
                } catch (IOException ignore) {
                    // best-effort rollback
                }
            }
        }

        /**
         * [FIX-3] 按原文件的行尾风格写回，并仅在原文件以换行结尾时补尾部换行，
         * 不再依赖平台默认分隔符、也不再无脑追加换行。
         */
        private void writeLines(Path target, List<String> lines, String lineSep, boolean trailingNewline) throws IOException {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                sb.append(lines.get(i));
                if (i < lines.size() - 1) {
                    sb.append(lineSep);
                }
            }
            if (trailingNewline && !lines.isEmpty()) {
                sb.append(lineSep);
            }
            Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        private String stripPrefix(String path) {
            if (path == null) return null;
            if (path.startsWith("a/") || path.startsWith("b/")) return path.substring(2);
            return path;
        }

        /**
         * [FIX-6] 判断 lines[i] 是否真正是一个文件头的起点。
         * 关键点：unified diff 的 "--- " 文件头紧接着一定是 "+++ " 行；
         * 而内容里被删除的 "-- a/x" 这类行（diff 中表现为 "--- a/x"）后面不会是 "+++ "，
         * 据此把"真实文件头"和"恰好长得像头的内容行"区分开。
         */
        private static boolean isFileHeaderStart(String[] lines, int i) {
            String l = lines[i];
            if (l.startsWith("diff --git")) {
                return true;
            }
            if (l.startsWith("--- ")) {
                return i + 1 < lines.length && lines[i + 1].startsWith("+++ ");
            }
            return false;
        }

        /**
         * 解码文件字节，UTF-8 优先、失败回退默认编码；保留行尾风格与结尾换行信息。
         * [FIX-4] 不再对全文做 NFC 归一化重写——只在 matchesAt 匹配时按需归一化，
         * 未改动的行原样保留，避免悄悄改写整篇文件的 Unicode 表示。
         */
        private static FileContent decodeContent(byte[] raw) {
            String text;
            try {
                text = decodeStrict(raw, StandardCharsets.UTF_8);
            } catch (CharacterCodingException e) {
                try {
                    text = decodeStrict(raw, Charset.defaultCharset());
                } catch (CharacterCodingException ex) {
                    text = new String(raw, Charset.defaultCharset());
                }
            }

            boolean trailingNewline = !text.isEmpty() && text.charAt(text.length() - 1) == '\n';
            String lineSep = text.contains("\r\n") ? "\r\n" : "\n";

            String norm = text.replace("\r\n", "\n").replace("\r", "\n");
            List<String> lines = new ArrayList<>(Arrays.asList(norm.split("\n", -1)));
            if (trailingNewline && !lines.isEmpty()) {
                // 去掉结尾换行产生的最后一个空元素，与 readAllLines 语义对齐。
                lines.remove(lines.size() - 1);
            }
            return new FileContent(lines, lineSep, trailingNewline);
        }

        private static String decodeStrict(byte[] raw, Charset charset) throws CharacterCodingException {
            CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(raw)).toString();
        }

        /**
         * 归一化 AI 输出的 patch：
         * 1. CRLF/CR -> LF，避免 \r 进入行内容导致匹配失败；
         * 2. 兼容 ```diff 代码块；
         * 3. [FIX-1] hunk 内的完全空行补空格前缀，标记为上下文行；
         * 4. [FIX-6] Unicode NFC 归一化，避免中文引号/emoji 等字符表示不一致导致匹配失败；
         * 5. 补齐结尾换行。
         */
        private String normalizePatch(String patch) {
            if (patch == null) {
                return "";
            }

            // [FIX-6] Unicode NFC 归一化
            String s = Normalizer.normalize(patch, Normalizer.Form.NFC);

            s = s.replace("\r\n", "\n").replace("\r", "\n");

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

            // [FIX-1] hunk 内的完全空行补空格前缀，
            // 避免 java-diff-utils 解析时丢失上下文行导致匹配失败。
            // [FIX-6] 文件头边界检测使用 isFileHeaderStart，区分内容行与真实文件头。
            // [FIX-10] split("\n", -1) 会把结尾换行拆出一个尾部空元素，它不是 patch 的真实行；
            // 若不跳过，会被 FIX-1 补成幽灵上下文行混入最后一个 hunk 的 source 块，
            // 导致 applyTo 与兜底搜索双双失败——这正是"以换行结尾的 patch 全部应用失败"的根因。
            String[] lines = s.split("\n", -1);
            int lineCount = lines.length;
            if (lineCount > 0 && lines[lineCount - 1].isEmpty()) {
                lineCount--;
            }
            StringBuilder sb = new StringBuilder();
            boolean inHunk = false;
            for (int i = 0; i < lineCount; i++) {
                String line = lines[i];
                if (line.startsWith("@@")) {
                    inHunk = true;
                } else if (isFileHeaderStart(lines, i)) {
                    inHunk = false;
                }
                if (inHunk && line.isEmpty()) {
                    sb.append(" ");
                } else {
                    sb.append(line);
                }
                sb.append("\n");
            }

            return sb.toString();
        }

        /**
         * 修正 hunk header 的行数部分，同时保留 AI 给的起始行号。
         * <p>
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

                        // [FIX-2/FIX-6] 多文件 patch 边界检测：
                        // 下一个 @@、或经 isFileHeaderStart 确认的真实文件头才算边界，
                        // 避免把内容里恰好以 "--- " 开头的删除行误判成新文件边界。
                        if (l.startsWith("@@") || isFileHeaderStart(rawLines, j)) {
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
         * [FIX-4] 从原始 patch 文本提取新建文件内容。
         * 统一用 stripPrefix 后的路径匹配，兼容有/无 b/ 前缀的情况。
         * [FIX-3] 记录是否以换行结尾（识别 "\ No newline at end of file"）。
         * [FIX-6] 文件边界检测使用 isFileHeaderStart。
         */
        private NewFileContent extractNewFileContent(UnifiedDiffFile file, String patchStr) {
            String stripped = stripPrefix(file.getToFile());

            List<String> content = new ArrayList<>();
            boolean foundFile = false;
            boolean inHunk = false;
            boolean trailingNewline = true;

            String[] lines = patchStr.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (!foundFile && line.startsWith("+++ ")) {
                    String pathInLine = stripPrefix(line.substring(4).trim());
                    if (pathInLine.equals(stripped)) {
                        foundFile = true;
                        continue;
                    }
                }

                if (foundFile && line.startsWith("@@")) {
                    inHunk = true;
                    continue;
                }

                if (inHunk) {
                    if (isFileHeaderStart(lines, i)) {
                        break;
                    }

                    if (line.startsWith("\\ No newline at end of file")) {
                        trailingNewline = false;
                        continue;
                    }

                    if (line.startsWith("+")) {
                        content.add(line.substring(1));
                    }
                }
            }

            if (content.isEmpty()) {
                file.getPatch().getDeltas().forEach(d -> content.addAll(d.getTarget().getLines()));
                trailingNewline = true;
            }

            return new NewFileContent(content, trailingNewline);
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
            wfm.writeContext(node, "stderr", result.failures().stream().map(f -> f.path() + ": " + f.reason()).reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b));
            NodeStatus status = result.success() ? NodeStatus.SUCCESS : NodeStatus.FAIL;
            wfm.write(node, new ToolCallContent("apply_patch", result.summary(), "", status, node, runId, config.id()));
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("apply_patch", result.summary(), config.toArguments(), status, node, runId, config.id()).withMeta(config.meta)));
        }

        private PatchConfig resolvePatchConfig(ApplyPatchNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object val = wfm.getContextVariable(node.params.getReference());
                if (val == null) return null;

                String id = null;
                String args = null;
                ToolCallMeta meta = ToolCallMeta.EMPTY;
                if (val instanceof JsonObject jo) {
                    id = jo.getString("id");
                    args = jo.getString("functionArguments");
                    meta = ToolCallMeta.from(jo);
                }
                if (args == null) return null;

                JsonObject parsed = new JsonObject(args);
                String patchContent = parsed.getString("patch");
                String toolPath = parsed.getString("path");

                Path workDir;
                if (!StringUtils.isEmpty(toolPath)) {
                    Path base = wfm.getWorkingDirectory();
                    createDirectoriesIfNeeded(base);
                    Path path = Path.of(toolPath);
                    workDir = path.isAbsolute() ? path : base.resolve(toolPath).normalize();
                } else {
                    workDir = resolveWorkDir(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
                }

                return new PatchConfig(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id, patchContent, workDir, false, meta);
            }

            String patchContent = resolveValue(node.params.getPatchLocation(), node.params.getPatchReference(), node.params.getPatch(), "patch", wfm);
            Path workDir = resolveWorkDir(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            return new PatchConfig(CommonUtils.uuid7().toString(), patchContent, workDir, true, ToolCallMeta.EMPTY);
        }

        private Path resolveWorkDir(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            Path base = wfm.getWorkingDirectory();
            createDirectoriesIfNeeded(base);
            String workDirStr = resolveValue(location, reference, customValue, "path", wfm);
            if (StringUtils.isEmpty(workDirStr)) return base;
            Path path = Path.of(workDirStr);
            return path.isAbsolute() ? path : base.resolve(workDirStr).normalize();
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

        private String resolveValue(String location, List<String> reference, String customValue, String fieldName, WorkFlowManage wfm) {
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