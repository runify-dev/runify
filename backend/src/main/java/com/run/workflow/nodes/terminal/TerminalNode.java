package com.run.workflow.nodes.terminal;

import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.ToolCallMeta;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.terminal.entity.TerminalNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 终端执行节点
 */
public class TerminalNode extends INode<TerminalNode, TerminalNodeData> {
    public final static String type = "terminal-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final int DEFAULT_TIMEOUT = 1800;

    private volatile Process process;

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        Process p = this.process;
        if (p != null && p.isAlive()) {
            killProcessTree(p);
        }
    }

    /**
     * 杀掉进程及其所有子进程（进程树）。
     * destroyForcibly() 只杀直接子进程（sh），sh 下面的实际命令会成为孤儿进程继续运行。
     */
    private static void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    record TerminalConfig(String id, String command, boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("command", command));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, TerminalNode, Supplier<List<Node>>> {

        private Supplier<List<Node>> invokeFail(WorkFlowManage wfm, TerminalNode node, TerminalConfig config, String runId, String result, String stderr, int exitCode, Throwable e) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            node.status = NodeStatus.FAIL;
            wfm.writeContext(node, "result", result);
            wfm.writeContext(node, "stdout", "");
            wfm.writeContext(node, "stderr", stderr);
            wfm.writeContext(node, "exitCode", exitCode);
            ToolCallContent tc = new ToolCallContent("run_command", result, "",
                    NodeStatus.FAIL, node, runId, id);
            if (config != null) tc.withMeta(config.meta());
            wfm.writeContext(node, "tool", JsonObject.mapFrom(tc));
            return node.handleFail(wfm, e);
        }

        private Supplier<List<Node>> invokeCancel(WorkFlowManage wfm, TerminalNode node, TerminalConfig config, String runId) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            node.status = NodeStatus.CANCELLED;
            wfm.writeContext(node, "result", "已取消");
            wfm.writeContext(node, "stdout", "");
            wfm.writeContext(node, "stderr", "");
            wfm.writeContext(node, "exitCode", -1);
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", "已取消", "",
                    NodeStatus.CANCELLED, node, runId, id)));
            return wfm.nextCancelNodeSupplier();
        }

        private CompletableFuture<String> readStreamAsync(InputStream is, Consumer<String> onChunk) {
            return CompletableFuture.supplyAsync(() -> {
                StringBuilder sb = new StringBuilder();
                try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        String chunk = new String(buf, 0, n);
                        sb.append(chunk);
                        if (onChunk != null) {
                            onChunk.accept(chunk);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return sb.toString();
            }, Executors.newVirtualThreadPerTaskExecutor());
        }

        /**
         * 收割读取流的 Future，防止 commonPool 线程泄漏。
         * destroyForcibly() 后管道关闭，read() 会很快返回，给 5 秒兜底。
         */
        private void cleanupFuture(CompletableFuture<?> future) {
            if (future == null) return;
            future.exceptionally(ex -> null);  // 吞掉预期的 IOException
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, TerminalNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            TerminalConfig config = resolveConfig(node.params, workFlowManage);
            int timeout = resolveTimeout(node.params, workFlowManage);

            // 启动前检查取消
            if (node.getStatus() == NodeStatus.CANCELLED) {
                return invokeCancel(workFlowManage, node, config, runId);
            }

            if (config == null || StringUtils.isEmpty(config.command())) {
                return invokeFail(workFlowManage, node, config, runId, "代码为空", "代码为空", 1, new RuntimeException("代码为空"));
            }

            CompletableFuture<String> stdoutFuture = null;
            CompletableFuture<String> stderrFuture = null;

            try {
                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("run_command", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
                }

                ProcessBuilder processBuilder = new ProcessBuilder();
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());

                File baseDir = new File(System.getProperty("user.home"), ".runify");
                File file = new File(baseDir, conversationId.toString());
                if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath())) {
                    throw new IllegalArgumentException("非法工作目录路径");
                }
                if (!file.exists()) {
                    file.mkdirs();
                }
                processBuilder.directory(file);
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    processBuilder.command("powershell", "-NoProfile", "-NonInteractive", "-Command", config.command());
                } else {
                    processBuilder.command("sh", "-c", config.command());
                }
                processBuilder.redirectErrorStream(false);

                Process process = processBuilder.start();
                node.process = process;

                // 启动后立刻检查（防止 start 和 cancel 交错）
                if (node.getStatus() == NodeStatus.CANCELLED) {
                    killProcessTree(process);
                    return invokeCancel(workFlowManage, node, config, runId);
                }

                stdoutFuture = readStreamAsync(process.getInputStream(), chunk ->
                        workFlowManage.write(node, new ToolCallContent("run_command", chunk, "",
                                NodeStatus.RUNNING, node, runId, config.id())));

                stderrFuture = readStreamAsync(process.getErrorStream(), null);

                // 分段等待，每秒检查取消标志
                boolean finished = false;
                for (int elapsed = 0; elapsed < timeout; elapsed++) {
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        killProcessTree(process);
                        return invokeCancel(workFlowManage, node, config, runId);
                    }
                    if (process.waitFor(1, TimeUnit.SECONDS)) {
                        finished = true;
                        break;
                    }
                }

                // 超时处理
                if (!finished) {
                    killProcessTree(process);
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        return invokeCancel(workFlowManage, node, config, runId);
                    }
                    String timeoutMsg = "命令执行超时（" + timeout + "秒）";
                    return invokeFail(workFlowManage, node, config, runId, timeoutMsg, timeoutMsg, -1, new RuntimeException(timeoutMsg));
                }

                // 进程结束后检查取消
                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return invokeCancel(workFlowManage, node, config, runId);
                }

                String stdout = stdoutFuture.get(timeout, TimeUnit.SECONDS);
                String stderr = stderrFuture.get(timeout, TimeUnit.SECONDS);

                int exitCode = process.exitValue();
                String result = exitCode == 0 ? stdout : (stderr.isEmpty() ? stdout : stderr);
                workFlowManage.writeContext(node, "result", result);
                workFlowManage.writeContext(node, "stdout", stdout);
                workFlowManage.writeContext(node, "stderr", stderr);
                workFlowManage.writeContext(node, "exitCode", exitCode);

                if (exitCode == 0) {
                    node.status = NodeStatus.SUCCESS;
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "",
                            NodeStatus.SUCCESS, node, runId, config.id()));
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", stdout, config.toArguments(),
                            NodeStatus.SUCCESS, node, runId, config.id()).withMeta(config.meta())));
                } else {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "",
                            NodeStatus.FAIL, node, runId, config.id()));
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", stderr, config.toArguments(),
                            NodeStatus.FAIL, node, runId, config.id()).withMeta(config.meta())));
                }

            } catch (Exception e) {
                Process p = node.process;
                if (p != null && p.isAlive()) {
                    killProcessTree(p);
                }
                // 异常时也检查取消
                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return invokeCancel(workFlowManage, node, config, runId);
                }
                return invokeFail(workFlowManage, node, config, runId, e.getMessage(), e.getMessage(), 1, e);
            } finally {
                node.process = null;
                // 所有路径（正常、取消、超时、异常）都收割 future，防止 commonPool 线程泄漏
                cleanupFuture(stdoutFuture);
                cleanupFuture(stderrFuture);
            }

            return workFlowManage.nextNodeSupplier(node.node.getId());
        }

        private TerminalConfig resolveConfig(TerminalNodeData data, WorkFlowManage wfm) {
            String location = data.getLocation();
            if ("tool_call".equals(location)) {
                return resolveFromRef(data.getReference(), wfm);
            }
            if ("reference".equals(data.getCodeLocation())) {
                return resolveFromRef(data.getCodeReference(), wfm);
            }
            return new TerminalConfig(CommonUtils.uuid7().toString(), data.getCode(), true, ToolCallMeta.EMPTY);
        }

        private TerminalConfig resolveFromRef(List<String> reference, WorkFlowManage wfm) {
            if (reference == null || reference.isEmpty()) return null;
            Object val = wfm.getContextVariable(reference);
            if (val == null) return null;
            if (val instanceof JsonObject jo) {
                ToolCallMeta meta = ToolCallMeta.from(jo);
                String id = jo.getString("id");
                String args = jo.getString("functionArguments");
                if (args != null) {
                    return new TerminalConfig(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                            new JsonObject(args).getString("command"), false, meta);
                }
                return new TerminalConfig(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        val.toString(), true, meta);
            }
            if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                JsonObject args = JacksonUtils.fromJson(call.getFunctionArguments(), JsonObject.class);
                return new TerminalConfig(call.getId(), args.getString("command"), false, ToolCallMeta.EMPTY);
            }
            if (val instanceof String v) {
                return new TerminalConfig(CommonUtils.uuid7().toString(), v, true, ToolCallMeta.EMPTY);
            }
            return null;
        }

        private int resolveTimeout(TerminalNodeData nodeData, WorkFlowManage wfm) {
            if (nodeData == null) return DEFAULT_TIMEOUT;
            String raw = resolveValue(nodeData.getTimeoutLocation(), nodeData.getTimeoutReference(),
                    nodeData.getTimeout() != null ? String.valueOf(nodeData.getTimeout()) : null, wfm);
            if (StringUtils.isEmpty(raw)) return DEFAULT_TIMEOUT;
            try {
                int val = Integer.parseInt(raw.trim());
                return val > 0 ? val : DEFAULT_TIMEOUT;
            } catch (NumberFormatException e) {
                return DEFAULT_TIMEOUT;
            }
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
    public TerminalNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        TerminalNodeData data = new TerminalNodeData();

        data.setLocation(jsonObject.getString("location"));
        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream()
                    .map(Object::toString)
                    .toList());
        }
        data.setCodeLocation(jsonObject.getString("codeLocation"));
        if (jsonObject.getJsonArray("codeReference") != null) {
            data.setCodeReference(jsonObject.getJsonArray("codeReference").stream()
                    .map(Object::toString)
                    .toList());
        }
        data.setCode(jsonObject.getString("code"));

        data.setTimeoutLocation(jsonObject.getString("timeoutLocation"));
        if (jsonObject.getJsonArray("timeoutReference") != null) {
            data.setTimeoutReference(jsonObject.getJsonArray("timeoutReference").stream()
                    .map(Object::toString)
                    .toList());
        }
        data.setTimeout(jsonObject.getInteger("timeout"));

        return data;
    }

    @Override
    public NodeResult<TerminalNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}