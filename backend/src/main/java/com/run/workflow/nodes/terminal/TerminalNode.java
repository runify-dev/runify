package com.run.workflow.nodes.terminal;

import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.ToolCallMeta;
import com.run.workflow.WorkFlowManage;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
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
import java.util.function.BooleanSupplier;
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
    private volatile String containerId;

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
        String cid = this.containerId;
        if (cid != null) {
            try {
                DockerClient client = DockerClientBuilder.getInstance().build();
                client.stopContainerCmd(cid).withTimeout(5).exec();
                client.close();
            } catch (Exception ignored) {
            }
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

        private void writeResult(WorkFlowManage wfm, TerminalNode node, TerminalConfig config,
                                 String runId, String stdout, String stderr, int exitCode) {
            String result = exitCode == 0 ? stdout : (stderr.isEmpty() ? stdout : stderr);
            NodeStatus status = exitCode == 0 ? NodeStatus.SUCCESS : NodeStatus.FAIL;
            String statusOutput = exitCode == 0 ? stdout : stderr;

            node.status = status;
            wfm.writeContext(node, "result", result);
            wfm.writeContext(node, "stdout", stdout);
            wfm.writeContext(node, "stderr", stderr);
            wfm.writeContext(node, "exitCode", exitCode);
            wfm.write(node, new ToolCallContent("run_command", "", "",
                    status, node, runId, config.id()));
            wfm.writeContext(node, "tool", JsonObject.mapFrom(
                    new ToolCallContent("run_command", statusOutput, config.toArguments(),
                            status, node, runId, config.id()).withMeta(config.meta())));
        }

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
            String runtime = node.params != null ? node.params.getRuntime() : null;

            // 启动前检查取消
            if (node.getStatus() == NodeStatus.CANCELLED) {
                return invokeCancel(workFlowManage, node, config, runId);
            }

            if (config == null || StringUtils.isEmpty(config.command())) {
                return invokeFail(workFlowManage, node, config, runId, "代码为空", "代码为空", 1, new RuntimeException("代码为空"));
            }

            if ("docker".equals(runtime)) {
                return executeInDocker(workFlowManage, node, config, runId, timeout);
            }

            CompletableFuture<String> stdoutFuture = null;
            CompletableFuture<String> stderrFuture = null;

            try {
                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("run_command", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
                }

                ProcessBuilder processBuilder = new ProcessBuilder();
                File file = workFlowManage.getWorkingDirectory().toFile();
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
                writeResult(workFlowManage, node, config, runId, stdout, stderr, exitCode);

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

        private static final String DOCKER_IMAGE = "ghcr.io/runify-dev/hush-toolbox:v0.1.0";

        private Supplier<List<Node>> executeInDocker(WorkFlowManage wfm, TerminalNode node, TerminalConfig config, String runId, int timeout) {
            if (config.withWriteArguments()) {
                wfm.write(node, new ToolCallContent("run_command", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
            }

            CompletableFuture<String> stdoutFuture = null;
            CompletableFuture<String> stderrFuture = null;

            try (DockerClient dc = DockerClientBuilder.getInstance().build()) {
                final DockerClient dockerClient = dc;

                String userHome = System.getProperty("user.home");
                UUID conversationId = (UUID) wfm.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                String workDir = userHome + "/.runify/" + conversationId;
                String caCertPath = userHome + "/.hush/hush-ca-public/ca.crt";

                dockerClient.pullImageCmd(DOCKER_IMAGE)
                        .exec(new PullImageResultCallback())
                        .awaitCompletion(timeout, TimeUnit.SECONDS);

                CreateContainerResponse container = dockerClient.createContainerCmd(DOCKER_IMAGE)
                        .withHostConfig(HostConfig.newHostConfig()
                                .withBinds(
                                        Bind.parse(caCertPath + ":/etc/hush/ca.crt:ro"),
                                        Bind.parse(workDir + ":/workspace")
                                )
                                .withAutoRemove(true))
                        .withEnv(
                                "HTTP_PROXY=http://host.docker.internal:25220",
                                "HTTPS_PROXY=http://host.docker.internal:25220",
                                "NO_PROXY=localhost,127.0.0.1"
                        )
                        .withWorkingDir("/workspace")
                        .withCmd("sh", "-c", config.command())
                        .exec();

                final String cid = container.getId();
                node.containerId = cid;

                if (checkCancelled(wfm, node, config, runId, dockerClient, cid)) {
                    return invokeCancel(wfm, node, config, runId);
                }

                StringBuilder stdoutSb = new StringBuilder();
                StringBuilder stderrSb = new StringBuilder();

                stdoutFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        dockerClient.logContainerCmd(cid)
                                .withStdOut(true).withStdErr(false).withFollowStream(true)
                                .exec(new LogContainerResultCallback() {
                                    @Override
                                    public void onNext(Frame frame) {
                                        String chunk = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                        stdoutSb.append(chunk);
                                        wfm.write(node, new ToolCallContent("run_command", chunk, "",
                                                NodeStatus.RUNNING, node, runId, config.id()));
                                    }
                                }).awaitCompletion(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return stdoutSb.toString();
                }, Executors.newVirtualThreadPerTaskExecutor());

                stderrFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        dockerClient.logContainerCmd(cid)
                                .withStdOut(false).withStdErr(true).withFollowStream(true)
                                .exec(new LogContainerResultCallback() {
                                    @Override
                                    public void onNext(Frame frame) {
                                        stderrSb.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                                    }
                                }).awaitCompletion(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return stderrSb.toString();
                }, Executors.newVirtualThreadPerTaskExecutor());

                dockerClient.startContainerCmd(cid).exec();

                boolean finished = waitForContainer(dockerClient, cid, timeout, () -> {
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        stopDockerContainer(dockerClient, cid);
                    }
                    return node.getStatus() == NodeStatus.CANCELLED;
                });

                if (!finished) {
                    stopDockerContainer(dockerClient, cid);
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        return invokeCancel(wfm, node, config, runId);
                    }
                    String timeoutMsg = "命令执行超时（" + timeout + "秒）";
                    return invokeFail(wfm, node, config, runId, timeoutMsg, timeoutMsg, -1, new RuntimeException(timeoutMsg));
                }

                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return invokeCancel(wfm, node, config, runId);
                }

                String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
                String stderr = stderrFuture.get(5, TimeUnit.SECONDS);

                var inspect = dockerClient.inspectContainerCmd(cid).exec();
                int exitCode = inspect.getState().getExitCode() != null ? inspect.getState().getExitCode() : -1;
                writeResult(wfm, node, config, runId, stdout, stderr, exitCode);

            } catch (Exception e) {
                if (node.getStatus() == NodeStatus.CANCELLED) {
                    return invokeCancel(wfm, node, config, runId);
                }
                return invokeFail(wfm, node, config, runId, e.getMessage(), e.getMessage(), 1, e);
            } finally {
                node.containerId = null;
                cleanupFuture(stdoutFuture);
                cleanupFuture(stderrFuture);
            }

            return wfm.nextNodeSupplier(node.node.getId());
        }

        private boolean checkCancelled(WorkFlowManage wfm, TerminalNode node, TerminalConfig config,
                                        String runId, DockerClient dockerClient, String containerId) {
            if (node.getStatus() == NodeStatus.CANCELLED) {
                stopDockerContainer(dockerClient, containerId);
                return true;
            }
            return false;
        }

        private boolean waitForContainer(DockerClient dockerClient, String containerId, int timeout,
                                          BooleanSupplier shouldStop) {
            for (int elapsed = 0; elapsed < timeout; elapsed++) {
                if (shouldStop.getAsBoolean()) return false;
                var inspect = dockerClient.inspectContainerCmd(containerId).exec();
                if (!inspect.getState().getRunning()) return true;
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        private static void stopDockerContainer(DockerClient dockerClient, String containerId) {
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public TerminalNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        TerminalNodeData data = new TerminalNodeData();

        data.setRuntime(jsonObject.getString("runtime"));
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