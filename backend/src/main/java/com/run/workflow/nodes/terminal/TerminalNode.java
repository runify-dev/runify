package com.run.workflow.nodes.terminal;

import com.run.common.keyvalue.DefaultKeyValue;
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

import java.util.List;
import java.util.Map;
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

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }


    public static class Handle implements BiFunction<WorkFlowManage, TerminalNode, Supplier<List<Node>>> {

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage workFlowManage, TerminalNode node) {
            String code = resolveValue(node.params.getLocation(), node.params.getReference(), node.params.getCode(), workFlowManage);
            int timeout = resolveTimeout(node.params, workFlowManage);

            if (StringUtils.isEmpty(code)) {
                node.status = NodeStatus.FAIL;
                workFlowManage.writeContext(node, "result", "代码为空");
                workFlowManage.writeContext(node, "stdout", "");
                workFlowManage.writeContext(node, "stderr", "代码为空");
                workFlowManage.writeContext(node, "exitCode", 1);
                return () -> workFlowManage.getNextList(node.node.getId()).stream().map(DefaultKeyValue::getValue).toList();
            }

            try {
                String id = CommonUtils.uuid7().toString();
                workFlowManage.write(node, new ToolCallContent("Terminal", "", code, NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"), id));

                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("sh", "-c", code);
                processBuilder.redirectErrorStream(false);

                Process process = processBuilder.start();

                // 实时读取标准输出
                StringBuilder stdoutBuilder = new StringBuilder();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdoutBuilder.append(line).append("\n");
                        workFlowManage.write(node, new ToolCallContent("", line + "\n", "", NodeStatus.RUNNING, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                    }
                }

                // 实时读取错误输出
                StringBuilder stderrBuilder = new StringBuilder();
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrBuilder.append(line).append("\n");
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
                    workFlowManage.write(node, new ToolCallContent("", "", "", NodeStatus.FAIL, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                    workFlowManage.writeContext(node, "tool", JacksonUtils.toJson(new ToolCallContent("Terminal", timeoutMsg, JacksonUtils.toJson(Map.of("code", code)), NodeStatus.FAIL, node, (String) workFlowManage.getParams().get("workflowRunId"), id)));
                } else {
                    int exitCode = process.exitValue();
                    workFlowManage.writeContext(node, "result", exitCode == 0 ? stdout : stderr);
                    workFlowManage.writeContext(node, "stdout", stdout);
                    workFlowManage.writeContext(node, "stderr", stderr);
                    workFlowManage.writeContext(node, "exitCode", exitCode);
                    node.status = exitCode == 0 ? NodeStatus.SUCCESS : NodeStatus.FAIL;
                    workFlowManage.write(node, new ToolCallContent("", "", "", node.status, node, (String) workFlowManage.getParams().get("workflowRunId"), id));
                    workFlowManage.writeContext(node, "tool", JacksonUtils.toJson(new ToolCallContent("Terminal", exitCode == 0 ? stdout : stderr, JacksonUtils.toJson(Map.of("code", code)), node.status, node, (String) workFlowManage.getParams().get("workflowRunId"), id)));
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
