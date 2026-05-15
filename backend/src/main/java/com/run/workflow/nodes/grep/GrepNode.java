package com.run.workflow.nodes.grep;

import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.grep.entity.GrepNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.util.*;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class GrepNode extends INode<GrepNode, GrepNodeData> {

    public final static String type = "grep-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", "node_modules", ".next", "dist", "build", "__pycache__", ".idea", ".vscode", "target"
    );
    private static final Set<String> BINARY_EXTS = Set.of(
            "png", "jpg", "jpeg", "gif", "ico", "svg", "woff", "woff2", "ttf", "eot",
            "zip", "tar", "gz", "rar", "7z", "pdf", "exe", "dll", "so", "dylib",
            "mp3", "mp4", "avi", "mov", "webp", "avif"
    );

    public GrepNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public GrepNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record GrepConfig(String chunkId, String pattern, String searchPath, String filePattern,
                      int contextLines, int maxResults, boolean withWriteArguments) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("pattern", pattern, "path", searchPath,
                    "file_pattern", filePattern != null ? filePattern : "*", "max_results", maxResults));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, GrepNode, Supplier<List<Node>>> {

        private record Match(String file, int lineNum, String line, List<String> contextBefore,
                             List<String> contextAfter) {
        }

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, GrepNode node, GrepConfig config, String runId, Throwable e) {
            node.status = NodeStatus.FAIL;
            String chunkId = config != null ? config.chunkId() : CommonUtils.uuid7().toString();
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("grep", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, chunkId)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, GrepNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            GrepConfig config = resolveGrepConfig(node, workFlowManage);

            if (config == null) {
                return invokeFail(workFlowManage, node, null, runId, new RuntimeException("配置解析失败"));
            }
            if (StringUtils.isEmpty(config.pattern())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("搜索模式为空"));
            }
            if (StringUtils.isEmpty(config.searchPath())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("搜索路径为空"));
            }

            List<Match> matches = new ArrayList<>();
            Set<String> matchedFiles = new LinkedHashSet<>();
            Set<String> permissionDenied = new LinkedHashSet<>();

            try {
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
                if (!Files.exists(basePath)) {
                    Files.createDirectories(basePath);
                }
                Path target = basePath.resolve(config.searchPath()).normalize();

                if (!Files.exists(target)) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("路径不存在: " + config.searchPath()));
                }

                Pattern regex;
                try {
                    regex = Pattern.compile(config.pattern(), Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("正则表达式错误: " + e.getMessage()));
                }
                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("grep", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                if (Files.isRegularFile(target)) {
                    searchFile(target, basePath, regex, config.contextLines(), config.maxResults(), matches, matchedFiles, permissionDenied, node, workFlowManage, config.chunkId(), runId);
                } else {
                    searchDir(target, basePath, regex, config.filePattern(), config.contextLines(), config.maxResults(), matches, matchedFiles, permissionDenied, node, workFlowManage, config.chunkId(), runId);
                }

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return workFlowManage.nextCancelNodeSupplier();
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < matches.size(); i++) {
                    Match m = matches.get(i);
                    if (i > 0) sb.append("\n");
                    for (String ctx : m.contextBefore) {
                        sb.append(m.file).append("-").append(m.lineNum - m.contextBefore.size() + m.contextBefore.indexOf(ctx) + 1).append("- ").append(ctx).append("\n");
                    }
                    sb.append(m.file).append(":").append(m.lineNum).append(": ").append(m.line).append("\n");
                    for (int j = 0; j < m.contextAfter.size(); j++) {
                        sb.append(m.file).append("-").append(m.lineNum + j + 1).append("- ").append(m.contextAfter.get(j)).append("\n");
                    }
                }

                if (!permissionDenied.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append("--- 权限不足 ---\n");
                    for (String path : permissionDenied) {
                        sb.append(path).append("\n");
                    }
                }

                String content = sb.toString().stripTrailing();
                String summary = matches.size() + " 处匹配, " + matchedFiles.size() + " 个文件"
                        + (permissionDenied.isEmpty() ? "" : ", " + permissionDenied.size() + " 个权限不足");
                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "matches", matches.size());
                workFlowManage.writeContext(node, "files", matchedFiles.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("grep", content, config.toArguments(),
                        NodeStatus.SUCCESS, node, runId, config.chunkId())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("grep", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));

            } catch (AccessDeniedException e) {
                permissionDenied.add(config.searchPath());
            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private GrepConfig resolveGrepConfig(GrepNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(node.params.getReference());
                JsonObject toolCall = toJsonObject(ref);
                if (toolCall == null) return null;

                String id = toolCall.getString("id");
                String args = toolCall.getString("functionArguments");
                if (StringUtils.isEmpty(args)) return null;

                JsonObject parsed = new JsonObject(args);
                String pattern = parsed.getString("pattern");
                String searchPath = parsed.getString("path");
                String filePattern = parsed.getString("file_pattern");
                int contextLines = parsed.getInteger("context_lines", 0);
                int maxResults = parsed.getInteger("max_results", 50);
                boolean withWriteArguments = StringUtils.isEmpty(id);
                String chunkId = withWriteArguments ? CommonUtils.uuid7().toString() : id;

                return new GrepConfig(chunkId, pattern, searchPath, filePattern, contextLines, maxResults, withWriteArguments);
            }

            String pattern = resolveValue(node.params.getPatternLocation(), node.params.getPatternReference(), node.params.getPattern(), wfm);
            String searchPath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            String filePattern = resolveValue(node.params.getFilePatternLocation(), node.params.getFilePatternReference(), node.params.getFilePattern(), wfm);
            String ctxStr = resolveValue(node.params.getContextLinesLocation(), node.params.getContextLinesReference(),
                    node.params.getContextLines() != null ? String.valueOf(node.params.getContextLines()) : null, wfm);
            int contextLines = parseInt(ctxStr, 0);
            String maxStr = resolveValue(node.params.getMaxResultsLocation(), node.params.getMaxResultsReference(),
                    node.params.getMaxResults() != null ? String.valueOf(node.params.getMaxResults()) : null, wfm);
            int maxResults = parseInt(maxStr, 50);

            return new GrepConfig(CommonUtils.uuid7().toString(), pattern, searchPath, filePattern, contextLines, maxResults, true);
        }

        private void searchDir(Path dir, Path basePath, Pattern regex, String filePattern, int ctx, int max,
                               List<Match> matches, Set<String> matchedFiles, Set<String> permissionDenied,
                               GrepNode node, WorkFlowManage wfm, String chunkId, String runId) throws AccessDeniedException {
            if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
            try (var stream = Files.list(dir)) {
                var it = stream.iterator();
                while (it.hasNext()) {
                    if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
                    Path entry = it.next();
                    String name = entry.getFileName().toString();
                    if (IGNORED_DIRS.contains(name) || name.startsWith(".")) continue;
                    if (Files.isDirectory(entry)) {
                        try {
                            searchDir(entry, basePath, regex, filePattern, ctx, max, matches, matchedFiles, permissionDenied, node, wfm, chunkId, runId);
                        } catch (AccessDeniedException e) {
                            permissionDenied.add(entry.toString());
                            wfm.write(node, new ToolCallContent("grep", "[权限不足] " + entry + "\n", "",
                                    NodeStatus.RUNNING, node, runId, chunkId));
                        }
                    } else if (Files.isRegularFile(entry)) {
                        String ext = getExtension(name);
                        if (BINARY_EXTS.contains(ext)) continue;
                        if (!StringUtils.isEmpty(filePattern) && !matchGlob(name, filePattern)) continue;
                        try {
                            searchFile(entry, basePath, regex, ctx, max, matches, matchedFiles, permissionDenied, node, wfm, chunkId, runId);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (AccessDeniedException e) {
                permissionDenied.add(dir.toString());
                wfm.write(node, new ToolCallContent("grep", "[权限不足] " + dir + "\n", "",
                        NodeStatus.RUNNING, node, runId, chunkId));
            } catch (IOException ignored) {
            }
        }

        private void searchFile(Path file, Path basePath, Pattern regex, int ctx, int max,
                                List<Match> matches, Set<String> matchedFiles, Set<String> permissionDenied,
                                GrepNode node, WorkFlowManage wfm, String chunkId, String runId) throws IOException {
            if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
            List<String> lines;
            try {
                lines = Files.readAllLines(file);
            } catch (AccessDeniedException e) {
                permissionDenied.add(file.toString());
                wfm.write(node, new ToolCallContent("grep", "[权限不足] " + file + "\n", "",
                        NodeStatus.RUNNING, node, runId, chunkId));
                return;
            }
            String relPath = file.startsWith(basePath) ? basePath.relativize(file).toString() : file.toString();

            for (int i = 0; i < lines.size() && matches.size() < max; i++) {
                if (node.getStatus() == NodeStatus.CANCELLED) return;
                if (regex.matcher(lines.get(i)).find()) {
                    matchedFiles.add(relPath);
                    List<String> before = new ArrayList<>();
                    List<String> after = new ArrayList<>();
                    for (int b = Math.max(0, i - ctx); b < i; b++) before.add(lines.get(b));
                    for (int a = i + 1; a <= Math.min(lines.size() - 1, i + ctx); a++) after.add(lines.get(a));
                    Match m = new Match(relPath, i + 1, lines.get(i), before, after);
                    matches.add(m);
                    StringBuilder chunk = new StringBuilder();
                    for (String cb : m.contextBefore) {
                        chunk.append(m.file).append("-").append(m.lineNum - m.contextBefore.size() + m.contextBefore.indexOf(cb) + 1).append("- ").append(cb).append("\n");
                    }
                    chunk.append(m.file).append(":").append(m.lineNum).append(": ").append(m.line).append("\n");
                    for (int j = 0; j < m.contextAfter.size(); j++) {
                        chunk.append(m.file).append("-").append(m.lineNum + j + 1).append("- ").append(m.contextAfter.get(j)).append("\n");
                    }
                    wfm.write(node, new ToolCallContent("grep", chunk.toString(), "",
                            NodeStatus.RUNNING, node, runId, chunkId));
                }
            }
        }

        private boolean matchGlob(String name, String pattern) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            return name.matches("(?i)" + regex);
        }

        private String getExtension(String name) {
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
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
    public GrepNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        GrepNodeData data = new GrepNodeData();
        data.setLocation(jsonObject.getString("location"));

        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }

        data.setPatternLocation(jsonObject.getString("patternLocation"));
        if (jsonObject.getJsonArray("patternReference") != null) {
            data.setPatternReference(jsonObject.getJsonArray("patternReference").stream().map(Object::toString).toList());
        }
        data.setPattern(jsonObject.getString("pattern"));

        data.setPathLocation(jsonObject.getString("pathLocation"));
        if (jsonObject.getJsonArray("pathReference") != null) {
            data.setPathReference(jsonObject.getJsonArray("pathReference").stream().map(Object::toString).toList());
        }
        data.setPath(jsonObject.getString("path"));

        data.setFilePatternLocation(jsonObject.getString("filePatternLocation"));
        if (jsonObject.getJsonArray("filePatternReference") != null) {
            data.setFilePatternReference(jsonObject.getJsonArray("filePatternReference").stream().map(Object::toString).toList());
        }
        data.setFilePattern(jsonObject.getString("filePattern"));

        data.setContextLinesLocation(jsonObject.getString("contextLinesLocation"));
        if (jsonObject.getJsonArray("contextLinesReference") != null) {
            data.setContextLinesReference(jsonObject.getJsonArray("contextLinesReference").stream().map(Object::toString).toList());
        }
        data.setContextLines(jsonObject.getInteger("contextLines"));

        data.setMaxResultsLocation(jsonObject.getString("maxResultsLocation"));
        if (jsonObject.getJsonArray("maxResultsReference") != null) {
            data.setMaxResultsReference(jsonObject.getJsonArray("maxResultsReference").stream().map(Object::toString).toList());
        }
        data.setMaxResults(jsonObject.getInteger("maxResults"));

        return data;
    }

    @Override
    public NodeResult<GrepNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
