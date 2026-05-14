package com.run.workflow.nodes.grep;

import com.run.common.keyvalue.DefaultKeyValue;
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
import java.nio.file.*;
import java.util.*;
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

    public static class Handle implements BiFunction<WorkFlowManage, GrepNode, Supplier<List<Node>>> {

        private record Match(String file, int lineNum, String line, List<String> contextBefore, List<String> contextAfter) {}

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, GrepNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");

            try {
                String pattern;
                String searchPath;
                String filePattern;
                int contextLines;
                int maxResults;

                if ("tool_call".equals(node.params.getLocation())) {
                    if (node.params.getReference() == null || node.params.getReference().isEmpty()) {
                        node.status = NodeStatus.FAIL;
                        workFlowManage.nextFailInvoke(node, new RuntimeException("未指定引用变量"));
                        return null;
                    }
                    Object ref = workFlowManage.getContextVariable(node.params.getReference());
                    JsonObject toolCall = toJsonObject(ref);
                    if (toolCall == null) {
                        node.status = NodeStatus.FAIL;
                        workFlowManage.nextFailInvoke(node, new RuntimeException("引用变量格式错误"));
                        return null;
                    }
                    String args = toolCall.getString("functionArguments");
                    if (StringUtils.isEmpty(args)) {
                        node.status = NodeStatus.FAIL;
                        workFlowManage.nextFailInvoke(node, new RuntimeException("functionArguments 为空"));
                        return null;
                    }
                    JsonObject parsed = new JsonObject(args);
                    pattern = parsed.getString("pattern");
                    searchPath = parsed.getString("path");
                    filePattern = parsed.getString("file_pattern");
                    contextLines = parsed.getInteger("context_lines", 0);
                    maxResults = parsed.getInteger("max_results", 50);
                } else {
                    pattern = resolveValue(node.params.getPatternLocation(), node.params.getPatternReference(), node.params.getPattern(), workFlowManage);
                    searchPath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), workFlowManage);
                    filePattern = resolveValue(node.params.getFilePatternLocation(), node.params.getFilePatternReference(), node.params.getFilePattern(), workFlowManage);
                    String ctxStr = resolveValue(node.params.getContextLinesLocation(), node.params.getContextLinesReference(),
                            node.params.getContextLines() != null ? String.valueOf(node.params.getContextLines()) : null, workFlowManage);
                    contextLines = parseInt(ctxStr, 0);
                    String maxStr = resolveValue(node.params.getMaxResultsLocation(), node.params.getMaxResultsReference(),
                            node.params.getMaxResults() != null ? String.valueOf(node.params.getMaxResults()) : null, workFlowManage);
                    maxResults = parseInt(maxStr, 50);
                }

                if (StringUtils.isEmpty(pattern)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("搜索模式为空"));
                    return null;
                }
                if (StringUtils.isEmpty(searchPath)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("搜索路径为空"));
                    return null;
                }

                Path basePath = Path.of(System.getProperty("user.dir"));
                Path target = basePath.resolve(searchPath).normalize();

                if (!Files.exists(target)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("路径不存在: " + searchPath));
                    return null;
                }

                Pattern regex;
                try {
                    regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("正则表达式错误: " + e.getMessage()));
                    return null;
                }

                List<Match> matches = new ArrayList<>();
                Set<String> matchedFiles = new LinkedHashSet<>();
                String argsJson = JacksonUtils.toJson(Map.of("pattern", pattern, "path", searchPath,
                        "file_pattern", filePattern != null ? filePattern : "*", "max_results", maxResults));
                String chunkId = CommonUtils.uuid7().toString();

                if (Files.isRegularFile(target)) {
                    searchFile(target, basePath, regex, contextLines, maxResults, matches, matchedFiles, node, workFlowManage, argsJson, chunkId, runId);
                } else {
                    searchDir(target, basePath, regex, filePattern, contextLines, maxResults, matches, matchedFiles, node, workFlowManage, argsJson, chunkId, runId);
                }

                // 格式化完整输出
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

                String content = sb.toString().stripTrailing();
                String summary = matches.size() + " 处匹配, " + matchedFiles.size() + " 个文件";

                ToolCallContent toolContent = new ToolCallContent("grep", content, argsJson,
                        NodeStatus.SUCCESS, node, runId, CommonUtils.uuid7().toString());

                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "matches", matches.size());
                workFlowManage.writeContext(node, "files", matchedFiles.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(toolContent));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, toolContent);

            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, e);
                return null;
            }

            return next(workFlowManage, node);
        }

        private void searchDir(Path dir, Path basePath, Pattern regex, String filePattern, int ctx, int max,
                              List<Match> matches, Set<String> matchedFiles, GrepNode node,
                              WorkFlowManage wfm, String argsJson, String chunkId, String runId) throws IOException {
            if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
            try (Stream<Path> stream = Files.walk(dir, 20)) {
                stream.filter(p -> {
                    if (!Files.isRegularFile(p)) return false;
                    String name = p.getFileName().toString();
                    for (Path part : dir.relativize(p)) {
                        String partName = part.toString();
                        if (IGNORED_DIRS.contains(partName) || partName.startsWith(".")) return false;
                    }
                    String ext = getExtension(name);
                    if (BINARY_EXTS.contains(ext)) return false;
                    if (!StringUtils.isEmpty(filePattern)) {
                        return matchGlob(name, filePattern);
                    }
                    return true;
                }).forEach(p -> {
                    if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
                    try {
                        searchFile(p, basePath, regex, ctx, max, matches, matchedFiles, node, wfm, argsJson, chunkId, runId);
                    } catch (IOException ignored) {}
                });
            }
        }

        private void searchFile(Path file, Path basePath, Pattern regex, int ctx, int max,
                               List<Match> matches, Set<String> matchedFiles, GrepNode node,
                               WorkFlowManage wfm, String argsJson, String chunkId, String runId) throws IOException {
            if (matches.size() >= max || node.getStatus() == NodeStatus.CANCELLED) return;
            List<String> lines = Files.readAllLines(file);
            String relPath = basePath.relativize(file).toString();

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
                    // 流式输出每个匹配
                    StringBuilder chunk = new StringBuilder();
                    for (String cb : m.contextBefore) {
                        chunk.append(m.file).append("-").append(m.lineNum - m.contextBefore.size() + m.contextBefore.indexOf(cb) + 1).append("- ").append(cb).append("\n");
                    }
                    chunk.append(m.file).append(":").append(m.lineNum).append(": ").append(m.line).append("\n");
                    for (int j = 0; j < m.contextAfter.size(); j++) {
                        chunk.append(m.file).append("-").append(m.lineNum + j + 1).append("- ").append(m.contextAfter.get(j)).append("\n");
                    }
                    wfm.write(node, new ToolCallContent("grep", chunk.toString(), argsJson,
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
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultVal; }
        }

        private Supplier<List<Node>> next(WorkFlowManage wfm, GrepNode node) {
            return () -> wfm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
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
