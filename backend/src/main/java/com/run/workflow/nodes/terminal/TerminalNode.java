package com.run.workflow.nodes.terminal;

import com.run.common.util.ChatCompletionAccumulator;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.terminal.entity.TerminalNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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

    private static final int DEFAULT_TIMEOUT = 30;

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
            p.destroyForcibly();
        }
    }


    record TerminalConfig(String id, String command, boolean withWriteArguments) {
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
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", result, "",
                    NodeStatus.FAIL, node, runId, id)));
            return node.handleFail(wfm, e);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, TerminalNode node) {
            String runId = (String) workFlowManage.getParams().get("workflowRunId");
            TerminalConfig config = resolveConfig(node.params, workFlowManage);
            int timeout = resolveTimeout(node.params, workFlowManage);

            if (config == null || StringUtils.isEmpty(config.command())) {
                return invokeFail(workFlowManage, node, config, runId, "代码为空", "代码为空", 1, new RuntimeException("代码为空"));
            }

            try {
                if (config.withWriteArguments()) {
                    workFlowManage.write(node, new ToolCallContent("run_command", "",
                            config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
                }

                ProcessBuilder processBuilder = new ProcessBuilder();
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());

                File file = new File(System.getProperty("user.home") + "/.runify/" + conversationId);
                if (!file.exists()) {
                    file.mkdirs();
                }
                processBuilder.directory(file);
                processBuilder.command("sh", "-c", config.command());
                processBuilder.redirectErrorStream(false);

                Process process = processBuilder.start();
                node.process = process;

                StringBuilder stdoutBuilder = new StringBuilder();
                try (var reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        String chunk = new String(buf, 0, n);
                        stdoutBuilder.append(chunk);
                        workFlowManage.write(node, new ToolCallContent("run_command", chunk, "",
                                NodeStatus.RUNNING, node, runId, config.id()));
                    }
                }

                StringBuilder stderrBuilder = new StringBuilder();
                try (var reader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        stderrBuilder.append(new String(buf, 0, n));
                    }
                }

                boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

                String stdout = stdoutBuilder.toString();
                String stderr = stderrBuilder.toString();

                if (!finished) {
                    process.destroyForcibly();
                    String timeoutMsg = "命令执行超时（" + timeout + "秒）";
                    return invokeFail(workFlowManage, node, config, runId, timeoutMsg, timeoutMsg, -1, new RuntimeException(timeoutMsg));
                }

                int exitCode = process.exitValue();
                String result = exitCode == 0 ? stdout : stderr;
                workFlowManage.writeContext(node, "result", result);
                workFlowManage.writeContext(node, "stdout", stdout);
                workFlowManage.writeContext(node, "stderr", stderr);
                workFlowManage.writeContext(node, "exitCode", exitCode);

                if (exitCode == 0) {
                    node.status = NodeStatus.SUCCESS;
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "",
                            NodeStatus.SUCCESS, node, runId, config.id()));
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", stdout, config.toArguments(),
                            NodeStatus.SUCCESS, node, runId, config.id())));
                } else {
                    node.status = NodeStatus.FAIL;
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "",
                            NodeStatus.FAIL, node, runId, config.id()));
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", stderr, config.toArguments(),
                            NodeStatus.FAIL, node, runId, config.id())));
                }

            } catch (Exception e) {
                return invokeFail(workFlowManage, node, config, runId, e.getMessage(), e.getMessage(), 1, e);
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
            return new TerminalConfig(CommonUtils.uuid7().toString(), data.getCode(), true);
        }

        private TerminalConfig resolveFromRef(List<String> reference, WorkFlowManage wfm) {
            if (reference == null || reference.isEmpty()) return null;
            Object val = wfm.getContextVariable(reference);
            if (val == null) return null;
            if (val instanceof JsonObject jo) {
                String id = jo.getString("id");
                String args = jo.getString("functionArguments");
                if (args != null) {
                    return new TerminalConfig(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                            new JsonObject(args).getString("command"), false);
                }
                return new TerminalConfig(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id,
                        val.toString(), true);
            }
            if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                JsonObject args = JacksonUtils.fromJson(call.getFunctionArguments(), JsonObject.class);
                return new TerminalConfig(call.getId(), args.getString("command"), false);
            }
            if (val instanceof String v) {
                return new TerminalConfig(CommonUtils.uuid7().toString(), v, true);
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

        // 超时配置
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
