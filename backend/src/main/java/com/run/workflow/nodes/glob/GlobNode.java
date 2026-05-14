package com.run.workflow.nodes.glob;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.glob.entity.GlobNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class GlobNode extends INode<GlobNode, GlobNodeData> {

    public final static String type = "glob-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final Set<String> IGNORED_DIRS = Set.of(
            ".git", "node_modules", ".next", "dist", "build", "__pycache__", ".idea", ".vscode", "target"
    );

    public GlobNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public GlobNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, GlobNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, GlobNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");

            try {
                String globPattern;
                String searchPath;
                int maxResults;

                AtomicReference<String> chunkId = new AtomicReference<>(CommonUtils.uuid7().toString());
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
                    chunkId.set(toolCall.getString("id"));
                    String args = toolCall.getString("functionArguments");
                    if (StringUtils.isEmpty(args)) {
                        node.status = NodeStatus.FAIL;
                        workFlowManage.nextFailInvoke(node, new RuntimeException("functionArguments 为空"));
                        return null;
                    }
                    JsonObject parsed = new JsonObject(args);
                    globPattern = parsed.getString("pattern");
                    searchPath = parsed.getString("path");
                    maxResults = parsed.getInteger("max_results", 1000);
                } else {
                    globPattern = resolveValue(node.params.getPatternLocation(), node.params.getPatternReference(), node.params.getPattern(), workFlowManage);
                    searchPath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), workFlowManage);
                    String maxStr = resolveValue(node.params.getMaxResultsLocation(), node.params.getMaxResultsReference(),
                            node.params.getMaxResults() != null ? String.valueOf(node.params.getMaxResults()) : null, workFlowManage);
                    maxResults = parseInt(maxStr, 1000);
                }

                if (StringUtils.isEmpty(globPattern)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("glob 模式为空"));
                    return null;
                }

                Path basePath = Path.of(System.getProperty("user.dir"));
                Path searchDir = StringUtils.isEmpty(searchPath) ? basePath : basePath.resolve(searchPath).normalize();

                if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.nextFailInvoke(node, new RuntimeException("目录不存在: " + searchPath));
                    return null;
                }

                // 构建 PathMatcher：同时匹配当前目录和递归子目录
                boolean hasSlash = globPattern.contains("/");
                final PathMatcher matcher;
                final PathMatcher rootMatcher;
                if (hasSlash) {
                    matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
                    rootMatcher = null;
                } else {
                    matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + globPattern);
                    rootMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
                }
                List<String> matched = new ArrayList<>();
                String argsJson = JacksonUtils.toJson(Map.of("pattern", globPattern, "path", searchPath != null ? searchPath : ".", "max_results", maxResults));


                Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        if (IGNORED_DIRS.contains(name) || name.startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (node.getStatus() == NodeStatus.CANCELLED) return FileVisitResult.TERMINATE;
                        if (matched.size() >= maxResults) return FileVisitResult.TERMINATE;
                        Path relative = searchDir.relativize(file);
                        boolean hit = matcher.matches(relative) || matcher.matches(file.getFileName())
                                || (rootMatcher != null && rootMatcher.matches(relative));
                        if (hit) {
                            String rel = basePath.relativize(file).toString();
                            matched.add(rel);
                            // 流式输出每个匹配项（不带 functionArguments，避免重复拼接）
                            workFlowManage.write(node, new ToolCallContent("glob", rel + "\n", "",
                                    NodeStatus.RUNNING, node, runId, chunkId.get()));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                String content = String.join("\n", matched);
                String summary = matched.size() + " 个文件";
                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "files", matched.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("glob", content, argsJson,
                        NodeStatus.SUCCESS, node, runId, chunkId.get())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("glob", "", "",
                        NodeStatus.SUCCESS, node, runId, chunkId.get()));
            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                workFlowManage.nextFailInvoke(node, e);
                return null;
            }

            return next(workFlowManage, node);
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

        private Supplier<List<Node>> next(WorkFlowManage wfm, GlobNode node) {
            return () -> wfm.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }
    }

    @Override
    public GlobNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        GlobNodeData data = new GlobNodeData();
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

        data.setMaxResultsLocation(jsonObject.getString("maxResultsLocation"));
        if (jsonObject.getJsonArray("maxResultsReference") != null) {
            data.setMaxResultsReference(jsonObject.getJsonArray("maxResultsReference").stream().map(Object::toString).toList());
        }
        data.setMaxResults(jsonObject.getInteger("maxResults"));

        return data;
    }

    @Override
    public NodeResult<GlobNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
