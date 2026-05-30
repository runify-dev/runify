package com.run.workflow.nodes.glob;

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
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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

    record GlobConfig(String chunkId, String globPattern, String searchPath, int maxResults,
                      boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("pattern", globPattern, "path", searchPath != null ? searchPath : ".", "max_results", maxResults));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, GlobNode, Supplier<List<Node>>> {

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, GlobNode node, GlobConfig config, String runId, Throwable e) {
            node.status = NodeStatus.FAIL;
            String chunkId = config != null ? config.chunkId() : CommonUtils.uuid7().toString();
            ToolCallContent tc = new ToolCallContent("glob", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, chunkId);
            if (config != null) tc.withMeta(config.meta());
            wfm.writeContext(node, "tool", JsonObject.mapFrom(tc));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, GlobNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            GlobConfig config = resolveGlobConfig(node, workFlowManage);

            if (config == null) {
                return invokeFail(workFlowManage, node, null, runId, new RuntimeException("配置解析失败"));
            }

            if (StringUtils.isEmpty(config.globPattern())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("glob 模式为空"));
            }

            try {
                Path basePath = workFlowManage.getWorkingDirectory();
                Path searchDir = StringUtils.isEmpty(config.searchPath()) ? basePath : basePath.resolve(config.searchPath()).normalize();

                if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("目录不存在: " + config.searchPath()));
                }

                String pattern = config.globPattern();
                boolean hasSlash = pattern.contains("/");
                final PathMatcher matcher;
                final PathMatcher rootMatcher;
                if (hasSlash) {
                    matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                    // **/*.html 不能匹配根目录下的 file.html，用 *.html 兜底
                    rootMatcher = pattern.startsWith("**/")
                            ? FileSystems.getDefault().getPathMatcher("glob:" + pattern.substring(3))
                            : null;
                } else {
                    matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + pattern);
                    rootMatcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                }

                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("glob", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                List<String> matched = new ArrayList<>();
                String searchPrefix = config.searchPath() != null ? config.searchPath() : ".";
                Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                    @Override
                    public @NonNull FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        if (IGNORED_DIRS.contains(name) || name.startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public @NonNull FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (node.getStatus() == NodeStatus.CANCELLED) return FileVisitResult.TERMINATE;
                        if (matched.size() >= config.maxResults()) return FileVisitResult.TERMINATE;
                        Path relative = searchDir.relativize(file);
                        boolean hit = matcher.matches(relative) || matcher.matches(file.getFileName())
                                || (rootMatcher != null && rootMatcher.matches(relative));
                        if (hit) {
                            String rel = searchPrefix + "/" + relative;
                            matched.add(rel);
                            workFlowManage.write(node, new ToolCallContent("glob", rel + "\n", "",
                                    NodeStatus.RUNNING, node, runId, config.chunkId()));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public @NonNull FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return workFlowManage.nextCancelNodeSupplier();
                }

                String content = String.join("\n", matched);
                String summary = matched.size() + " 个文件";
                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "files", matched.size());
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("glob", content, config.toArguments(),
                        NodeStatus.SUCCESS, node, runId, config.chunkId()).withMeta(config.meta())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("glob", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));
            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private GlobConfig resolveGlobConfig(GlobNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(node.params.getReference());
                JsonObject toolCall = toJsonObject(ref);
                if (toolCall == null) return null;

                String id = toolCall.getString("id");
                String args = toolCall.getString("functionArguments");
                if (StringUtils.isEmpty(args)) return null;

                JsonObject parsed = new JsonObject(args);
                String globPattern = parsed.getString("pattern");
                String searchPath = parsed.getString("path");
                int maxResults = parsed.getInteger("max_results", 1000);
                boolean withWriteArguments = StringUtils.isEmpty(id);
                String chunkId = withWriteArguments ? CommonUtils.uuid7().toString() : id;
                ToolCallMeta meta = ToolCallMeta.from(toolCall);

                return new GlobConfig(chunkId, globPattern, searchPath, maxResults, withWriteArguments, meta);
            }

            String globPattern = resolveValue(node.params.getPatternLocation(), node.params.getPatternReference(), node.params.getPattern(), wfm);
            String searchPath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            String maxStr = resolveValue(node.params.getMaxResultsLocation(), node.params.getMaxResultsReference(),
                    node.params.getMaxResults() != null ? String.valueOf(node.params.getMaxResults()) : null, wfm);
            int maxResults = parseInt(maxStr, 1000);

            return new GlobConfig(CommonUtils.uuid7().toString(), globPattern, searchPath, maxResults, true, ToolCallMeta.EMPTY);
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
