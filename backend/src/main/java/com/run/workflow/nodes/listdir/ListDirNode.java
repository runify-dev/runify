package com.run.workflow.nodes.listdir;

import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.listdir.entity.ListDirNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ListDirNode extends INode<ListDirNode, ListDirNodeData> {

    public final static String type = "list-dir-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public ListDirNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public ListDirNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    record ListDirConfig(String chunkId, String dirPath, int maxDepth, boolean withWriteArguments) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("path", dirPath, "depth", maxDepth));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, ListDirNode, Supplier<List<Node>>> {

        private static final Set<String> IGNORED_DIRS = Set.of(
                ".git", "node_modules", ".next", "dist", "build", "__pycache__", ".idea", ".vscode", "target"
        );

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, ListDirNode node, ListDirConfig config, String runId, Throwable e) {
            String chunkId = config != null ? config.chunkId() : CommonUtils.uuid7().toString();
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("list_dir", e.getMessage(), "",
                    NodeStatus.FAIL, node, runId, chunkId)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, ListDirNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            ListDirConfig config = resolveListDirConfig(node, workFlowManage);

            if (config == null) {
                return invokeFail(workFlowManage, node, null, runId, new RuntimeException("配置解析失败"));
            }
            if (StringUtils.isEmpty(config.dirPath())) {
                return invokeFail(workFlowManage, node, config, runId, new RuntimeException("目录路径为空"));
            }

            try {
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                Path basePath = Path.of(System.getProperty("user.home") + "/.runify/" + conversationId);
                if (!Files.exists(basePath)) {
                    Files.createDirectories(basePath);
                }
                Path targetDir = basePath.resolve(config.dirPath()).normalize();

                if (!Files.exists(targetDir)) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("目录不存在: " + config.dirPath()));
                }

                if (!Files.isDirectory(targetDir)) {
                    return invokeFail(workFlowManage, node, config, runId, new RuntimeException("不是目录: " + config.dirPath()));
                }

                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("list_dir", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.chunkId()));
                }

                StringBuilder tree = new StringBuilder();
                int[] fileCount = {0};
                int[] dirCount = {0};

                String rootLine = (config.dirPath().isEmpty() ? "." : config.dirPath()) + "/\n";
                tree.append(rootLine);
                workFlowManage.write(node, new ToolCallContent("list_dir", rootLine, "",
                        NodeStatus.RUNNING, node, runId, config.chunkId()));
                buildTree(targetDir, tree, "", config.maxDepth(), 0, fileCount, dirCount, workFlowManage, node, config.chunkId(), runId);

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return workFlowManage.nextCancelNodeSupplier();
                }

                String content = tree.toString();
                String summary = dirCount[0] + " 个目录, " + fileCount[0] + " 个文件";

                workFlowManage.writeContext(node, "content", content);
                workFlowManage.writeContext(node, "summary", summary);
                workFlowManage.writeContext(node, "files", fileCount[0]);
                workFlowManage.writeContext(node, "dirs", dirCount[0]);
                workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("list_dir", content, config.toArguments(),
                        NodeStatus.SUCCESS, node, runId, config.chunkId())));
                node.status = NodeStatus.SUCCESS;

                workFlowManage.write(node, new ToolCallContent("list_dir", "", "",
                        NodeStatus.SUCCESS, node, runId, config.chunkId()));
            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private ListDirConfig resolveListDirConfig(ListDirNode node, WorkFlowManage wfm) {
            if ("tool_call".equals(node.params.getLocation())) {
                if (node.params.getReference() == null || node.params.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(node.params.getReference());
                JsonObject toolCall = toJsonObject(ref);
                if (toolCall == null) return null;

                String id = toolCall.containsKey("id") ? toolCall.getString("id") : null;
                String args = toolCall.getString("functionArguments");
                if (StringUtils.isEmpty(args)) return null;

                JsonObject parsed = new JsonObject(args);
                String dirPath = parsed.getString("path");
                int maxDepth = parsed.getInteger("depth", 3);
                boolean withWriteArguments = StringUtils.isEmpty(id);
                String chunkId = withWriteArguments ? CommonUtils.uuid7().toString() : id;

                return new ListDirConfig(chunkId, dirPath, maxDepth, withWriteArguments);
            }

            String dirPath = resolveValue(node.params.getPathLocation(), node.params.getPathReference(), node.params.getPath(), wfm);
            String depthStr = resolveValue(node.params.getDepthLocation(), node.params.getDepthReference(),
                    node.params.getDepth() != null ? String.valueOf(node.params.getDepth()) : null, wfm);
            int maxDepth = parseInt(depthStr, 3);

            return new ListDirConfig(CommonUtils.uuid7().toString(), dirPath, maxDepth, true);
        }

        private void buildTree(Path dir, StringBuilder tree, String prefix, int maxDepth, int currentDepth,
                               int[] fileCount, int[] dirCount, WorkFlowManage wfm, ListDirNode node,
                               String chunkId, String runId) throws IOException {
            if (currentDepth >= maxDepth) return;

            List<Path> entries;
            try (Stream<Path> stream = Files.list(dir)) {
                entries = stream
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return !IGNORED_DIRS.contains(name) && !name.startsWith(".");
                        })
                        .sorted((a, b) -> {
                            boolean aDir = Files.isDirectory(a);
                            boolean bDir = Files.isDirectory(b);
                            if (aDir != bDir) return aDir ? -1 : 1;
                            return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                        })
                        .toList();
            } catch (AccessDeniedException e) {
                String line = prefix + "└── [permission denied]\n";
                tree.append(line);
                wfm.write(node, new ToolCallContent("list_dir", line, "",
                        NodeStatus.RUNNING, node, runId, chunkId));
                return;
            }

            for (int i = 0; i < entries.size(); i++) {
                if (node.getStatus() == NodeStatus.CANCELLED) return;
                Path entry = entries.get(i);
                boolean isLast = (i == entries.size() - 1);
                String connector = isLast ? "└── " : "├── ";
                String childPrefix = isLast ? "    " : "│   ";
                String name = entry.getFileName().toString();

                try {
                    if (Files.isDirectory(entry)) {
                        dirCount[0]++;
                        String line = prefix + connector + name + "/\n";
                        tree.append(line);
                        wfm.write(node, new ToolCallContent("list_dir", line, "",
                                NodeStatus.RUNNING, node, runId, chunkId));
                        buildTree(entry, tree, prefix + childPrefix, maxDepth, currentDepth + 1,
                                fileCount, dirCount, wfm, node, chunkId, runId);
                    } else {
                        fileCount[0]++;
                        long size = Files.size(entry);
                        String line = prefix + connector + name + "  " + formatSize(size) + "\n";
                        tree.append(line);
                        wfm.write(node, new ToolCallContent("list_dir", line, "",
                                NodeStatus.RUNNING, node, runId, chunkId));
                    }
                } catch (AccessDeniedException e) {
                    String line = prefix + connector + name + "  [permission denied]\n";
                    tree.append(line);
                    wfm.write(node, new ToolCallContent("list_dir", line, "",
                            NodeStatus.RUNNING, node, runId, chunkId));
                }
            }
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
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
    public ListDirNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        ListDirNodeData data = new ListDirNodeData();
        data.setLocation(jsonObject.getString("location"));

        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }

        data.setPathLocation(jsonObject.getString("pathLocation"));
        if (jsonObject.getJsonArray("pathReference") != null) {
            data.setPathReference(jsonObject.getJsonArray("pathReference").stream().map(Object::toString).toList());
        }
        data.setPath(jsonObject.getString("path"));

        data.setDepthLocation(jsonObject.getString("depthLocation"));
        if (jsonObject.getJsonArray("depthReference") != null) {
            data.setDepthReference(jsonObject.getJsonArray("depthReference").stream().map(Object::toString).toList());
        }
        data.setDepth(jsonObject.getInteger("depth"));

        return data;
    }

    @Override
    public NodeResult<ListDirNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
