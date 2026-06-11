package com.run.workflow.nodes.terminal;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.run_command.CommandResult;
import com.run.common.run_command.CommandRunner;
import com.run.common.run_command.impl.DockerCommandRunner;
import com.run.common.run_command.impl.LocalCommandRunner;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 终端执行节点（异步：apply 启动 runner 后立即返回 null，由回调推进工作流）
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

    /**
     * 当前正在执行的 runner（本地或 docker），供 cancel() 跨线程中止使用
     */
    private volatile CommandRunner runner;

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public TerminalNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        CommandRunner r = this.runner;
        if (r != null) {
            r.kill();
        }
    }

    record TerminalConfig(String id, String command, boolean withWriteArguments, ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("command", command));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, TerminalNode, Supplier<List<Node>>> {

        /**
         * 命令执行结束（进程/容器退出，含非 0 退出码）：写结果 + 走正常 next
         */
        private void complete(WorkFlowManage wfm, TerminalNode node, TerminalConfig config,
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

            wfm.nextInvoke(node, () -> wfm.getNextList(node.node.getId()).stream()
                    .map(DefaultKeyValue::getValue).toList());
        }

        /**
         * 异常 / 超时 / 启动前校验失败：写失败上下文 + 走失败 next
         */
        private void fail(WorkFlowManage wfm, TerminalNode node, TerminalConfig config, String runId,
                          String result, String stderr, int exitCode, Throwable e) {
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

            wfm.nextFailInvoke(node, e);
        }

        /**
         * 取消：写取消上下文 + 走取消 next
         */
        private void cancel(WorkFlowManage wfm, TerminalNode node, TerminalConfig config, String runId) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            node.status = NodeStatus.CANCELLED;
            wfm.writeContext(node, "result", "已取消");
            wfm.writeContext(node, "stdout", "");
            wfm.writeContext(node, "stderr", "");
            wfm.writeContext(node, "exitCode", -1);
            wfm.writeContext(node, "tool", JsonObject.mapFrom(new ToolCallContent("run_command", "已取消", "",
                    NodeStatus.CANCELLED, node, runId, id)));

            // ⚠️ 取消推进：你只给了成功(nextInvoke)/失败(nextFailInvoke)，没给取消。
            // 原来是 return wfm.nextCancelNodeSupplier();，下面按异步模型套了一层 nextInvoke，待确认。
            wfm.nextInvoke(node, wfm.nextCancelNodeSupplier());
        }

        /**
         * 根据 runtime 构造对应 runner。runner 只吃基本值，不认识 wfm。
         */
        private CommandRunner createRunner(WorkFlowManage wfm, TerminalConfig config, String runtime, int timeout) {
            // run_command 无 env；run_skill 接入时在此处注入解出来的 skill env
            Map<String, String> env = Map.of();
            if ("docker".equals(runtime)) {
                String userHome = System.getProperty("user.home");
                UUID conversationId = (UUID) wfm.getParams().getOrDefault("conversationId", CommonUtils.uuid7());
                String workDir = userHome + "/.runify/" + conversationId;
                return new DockerCommandRunner(config.command(), env, workDir, timeout);
            }
            return new LocalCommandRunner(config.command(), env, wfm.getWorkingDirectory(), timeout);
        }

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage wfm, TerminalNode node) {
            String runId = (String) wfm.getParams().get("workflowRunId");
            TerminalConfig config = resolveConfig(node.params, wfm);
            int timeout = resolveTimeout(node.params, wfm);
            String runtime = node.params != null ? node.params.getRuntime() : null;

            // 启动前检查取消
            if (node.getStatus() == NodeStatus.CANCELLED) {
                cancel(wfm, node, config, runId);
                return null;
            }

            if (config == null || StringUtils.isEmpty(config.command())) {
                fail(wfm, node, config, runId, "代码为空", "代码为空", 1, new RuntimeException("代码为空"));
                return null;
            }

            final TerminalConfig cfg = config;
            if (cfg.withWriteArguments()) {
                wfm.write(node, new ToolCallContent("run_command", "",
                        cfg.toArguments(), NodeStatus.RUNNING, node, runId, cfg.id()));
            }

            CommandRunner runner = createRunner(wfm, cfg, runtime, timeout);
            node.runner = runner;

            runner.run(new CommandRunner.Listener() {
                @Override
                public void onNext(String chunk) {
                    wfm.write(node, new ToolCallContent("run_command", chunk, "",
                            NodeStatus.RUNNING, node, runId, cfg.id()));
                }


                @Override
                public void onComplete(CommandResult result) {
                    node.runner = null;
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        cancel(wfm, node, cfg, runId);
                        return;
                    }
                    complete(wfm, node, cfg, runId, result.stdout(), result.stderr(), result.exitCode());
                }

                @Override
                public void onError(Throwable e) {
                    node.runner = null;
                    if (node.getStatus() == NodeStatus.CANCELLED) {
                        cancel(wfm, node, cfg, runId);
                        return;
                    }
                    fail(wfm, node, cfg, runId, e.getMessage(), e.getMessage(), 1, e);
                }
            });

            // 异步节点：执行结果由上面的回调通过 nextInvoke / nextFailInvoke 推进，这里不返回 next
            return null;
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