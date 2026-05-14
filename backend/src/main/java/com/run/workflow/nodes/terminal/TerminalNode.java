package com.run.workflow.nodes.terminal;

import com.run.common.keyvalue.DefaultKeyValue;
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


    public static class Handle implements BiFunction<WorkFlowManage, TerminalNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, TerminalNode node) {
            Code code;
            if ("tool_call".equals(node.params.getLocation())) {
                code = resolveCode("tool_call", node.params.getReference(), null, workFlowManage);
            } else {
                // customize 模式：用 codeLocation/codeReference 解析代码
                code = resolveCode(node.params.getCodeLocation(), node.params.getCodeReference(), node.params.getCode(), workFlowManage);
            }

            int timeout = resolveTimeout(node.params, workFlowManage);

            if (code == null || StringUtils.isEmpty(code.command)) {
                node.status = NodeStatus.FAIL;
                workFlowManage.writeContext(node, "result", "代码为空");
                workFlowManage.writeContext(node, "stdout", "");
                workFlowManage.writeContext(node, "stderr", "代码为空");
                workFlowManage.writeContext(node, "exitCode", 1);
                workFlowManage.nextFailInvoke(node, new RuntimeException("代码为空"));
                return null;
            }

            try {
                String id = code.id;
                if (code.withWriteArguments) {
                    workFlowManage.write(node, new ToolCallContent("run_command", "", code.command, NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                }
                ProcessBuilder processBuilder = new ProcessBuilder();
                UUID conversationId = (UUID) workFlowManage.getParams().getOrDefault("conversationId", CommonUtils.uuid7());

                File file = new File(System.getProperty("user.home") + "/.runify/" + conversationId);
                if (!file.exists()) {
                    file.mkdirs();
                }
                processBuilder.directory(file);
                processBuilder.command("sh", "-c", code.command);
                processBuilder.redirectErrorStream(false);

                Process process = processBuilder.start();
                node.process = process;

                // 实时读取标准输出
                StringBuilder stdoutBuilder = new StringBuilder();
                try (var reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        String chunk = new String(buf, 0, n);
                        stdoutBuilder.append(chunk);
                        workFlowManage.write(node, new ToolCallContent(
                                "run_command", chunk, "", NodeStatus.RUNNING, node,
                                (String) workFlowManage.getParams().get("workflowRunId"), id
                        ));
                    }
                }

                // 实时读取错误输出
                StringBuilder stderrBuilder = new StringBuilder();
                try (var reader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        String chunk = new String(buf, 0, n);
                        stderrBuilder.append(chunk);
                    }
                }

                // 等待执行完成（带超时）
                boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);

                String stdout = stdoutBuilder.toString();
                String stderr = stderrBuilder.toString();

                if (!finished) {
                    // 超时：强杀进程
                    process.destroyForcibly();
                    String timeoutMsg = "命令执行超时（" + timeout + "秒）";
                    node.status = NodeStatus.FAIL;
                    workFlowManage.writeContext(node, "result", timeoutMsg);
                    workFlowManage.writeContext(node, "stdout", stdout);
                    workFlowManage.writeContext(node, "stderr", timeoutMsg);
                    workFlowManage.writeContext(node, "exitCode", -1);
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "", NodeStatus.FAIL, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                    ToolCallContent toolCallContent = new ToolCallContent("run_command", timeoutMsg, JacksonUtils.toJson(Map.of("code", code)), NodeStatus.FAIL, node, (String) workFlowManage.getParams().get("workflowRunId"), id);
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(toolCallContent));
                } else {
                    int exitCode = process.exitValue();
                    workFlowManage.writeContext(node, "result", exitCode == 0 ? stdout : stderr);
                    workFlowManage.writeContext(node, "stdout", stdout);
                    workFlowManage.writeContext(node, "stderr", stderr);
                    workFlowManage.writeContext(node, "exitCode", exitCode);
                    node.status = exitCode == 0 ? NodeStatus.SUCCESS : NodeStatus.FAIL;
                    workFlowManage.write(node, new ToolCallContent("run_command", "", "", node.status, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                    workFlowManage.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", exitCode == 0 ? stdout : stderr, JacksonUtils.toJson(Map.of("command", code.command)), node.status, node, (String) workFlowManage.getParams().get("workflowRunId"), id)));
                }
            } catch (Exception e) {
                node.status = NodeStatus.FAIL;
                workFlowManage.writeContext(node, "result", e.getMessage());
                workFlowManage.writeContext(node, "stdout", "");
                workFlowManage.writeContext(node, "stderr", e.getMessage());
                workFlowManage.writeContext(node, "exitCode", 1);
            }

            return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
        }

        /**
         * 解析超时时间
         */
        private int resolveTimeout(TerminalNodeData nodeData, WorkFlowManage workFlowManage) {
            if (nodeData == null) return DEFAULT_TIMEOUT;

            String raw = resolveValue(nodeData.getTimeoutLocation(), nodeData.getTimeoutReference(),
                    nodeData.getTimeout() != null ? String.valueOf(nodeData.getTimeout()) : null, workFlowManage);
            if (StringUtils.isEmpty(raw)) return DEFAULT_TIMEOUT;

            try {
                int val = Integer.parseInt(raw.trim());
                return val > 0 ? val : DEFAULT_TIMEOUT;
            } catch (NumberFormatException e) {
                return DEFAULT_TIMEOUT;
            }
        }

        public record Code(String id, String command, Boolean withWriteArguments) {

        }

        /**
         * 通用值解析：reference 从上下文取，customize 直接返回
         */
        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage workFlowManage) {
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = workFlowManage.getContextVariable(reference);
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }

        /**
         * 通用值解析：reference 从上下文取，customize 直接返回
         */
        private Code resolveCode(String location, List<String> reference, String customValue, WorkFlowManage workFlowManage) {
            if ("tool_call".equals(location)) {
                if (reference == null || reference.isEmpty()) return null;
                Object val = workFlowManage.getContextVariable(reference);
                if (val == null) return null;
                String args = null;
                String id = null;
                if (val instanceof JsonObject jo) {
                    args = jo.getString("functionArguments");
                    id = jo.getString("id");
                } else if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                    args = call.getFunctionArguments();
                }
                if (args != null) {
                    try {
                        JsonObject parsed = new JsonObject(args);
                        return new Code(StringUtils.isEmpty(id) ? CommonUtils.uuid7().toString() : id, parsed.getString("command"), Boolean.FALSE);
                    } catch (Exception ignored) {
                    }
                }
                return new Code(CommonUtils.uuid7().toString(), val.toString(), Boolean.TRUE);
            }
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = workFlowManage.getContextVariable(reference);
                    if (val instanceof JsonObject v) {
                        String id = v.getString("id");
                        JsonObject string = JacksonUtils.fromJson(v.getString("arguments"), JsonObject.class);
                        return new Code(id, string.getString("command"), Boolean.FALSE);
                    }
                    if (val instanceof String v) {
                        return new Code(CommonUtils.uuid7().toString(), v, Boolean.TRUE);
                    }
                    if (val instanceof ChatCompletionAccumulator.AccumulatedToolCall call) {
                        JsonObject string = JacksonUtils.fromJson(call.getFunctionArguments(), JsonObject.class);
                        return new Code(call.getId(), string.getString("command"), Boolean.FALSE);
                    }
                    return null;
                }
                return null;
            }
            return new Code(CommonUtils.uuid7().toString(), customValue, Boolean.TRUE);
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
