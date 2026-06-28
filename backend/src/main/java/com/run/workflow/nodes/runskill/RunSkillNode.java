package com.run.workflow.nodes.runskill;

import com.run.common.keyvalue.DefaultKeyValue;
import com.run.common.run_command.CommandRunner;
import com.run.common.run_command.CommandResult;
import com.run.common.run_command.impl.DockerCommandRunner;
import com.run.common.run_command.impl.LocalCommandRunner;
import com.run.workflow.nodes.terminal.TerminalNode;
import io.vertx.core.Future;
import com.run.RunApplication;
import com.run.common.util.CommonUtils;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Skill;
import com.run.dao.mapper.SkillMapper;
import com.run.workflow.*;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.ToolCallContent;
import com.run.workflow.nodes.runskill.entity.RunSkillNodeData;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class RunSkillNode extends INode<RunSkillNode, RunSkillNodeData> {

    public final static String type = "run-skill-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    private static final int DEFAULT_TIMEOUT = 1800;
    private volatile CommandRunner runner;

    public RunSkillNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public RunSkillNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    @Override
    public void cancel() {
        super.cancel();
        CommandRunner r = this.runner;
        if (r != null) r.kill();
    }

    record RunSkillConfig(String id, String skillId, String command, String runtime, boolean withWriteArguments,
                          ToolCallMeta meta) {
        String toArguments() {
            return JacksonUtils.toJson(Map.of("skill_id", skillId, "command", command));
        }
    }

    public static class Handle implements BiFunction<WorkFlowManage, RunSkillNode, Supplier<List<Node>>> {

        private void complete(WorkFlowManage wfm, RunSkillNode node, RunSkillConfig config,
                              String runId, String stdout, String stderr, int exitCode) {
            String result = exitCode == 0 ? stdout : (stderr.isEmpty() ? stdout : stderr);
            NodeStatus status = exitCode == 0 ? NodeStatus.SUCCESS : NodeStatus.FAIL;
            String statusOutput = exitCode == 0 ? stdout : stderr;

            node.status = status;
            wfm.writeContext(node, "result", result);
            wfm.writeContext(node, "stdout", stdout);
            wfm.writeContext(node, "stderr", stderr);
            wfm.writeContext(node, "exitCode", exitCode);
            wfm.write(node, new ToolCallContent("run_skill", "", "",
                    status, node, runId, config.id()));
            wfm.writeContext(node, "tool", JsonObject.mapFrom(
                    new ToolCallContent("run_skill", statusOutput, config.toArguments(),
                            status, node, runId, config.id()).withMeta(config.meta())));

            wfm.nextInvoke(node, wfm.nextNodeSupplier(node.node.getId()));
        }

        private void fail(WorkFlowManage wfm, RunSkillNode node, RunSkillConfig config, String runId,
                          String result, String stderr, int exitCode, Throwable e) {
            String id = config != null ? config.id() : CommonUtils.uuid7().toString();
            node.status = NodeStatus.FAIL;
            wfm.writeContext(node, "result", result);
            wfm.writeContext(node, "stdout", "");
            wfm.writeContext(node, "stderr", stderr);
            wfm.writeContext(node, "exitCode", exitCode);
            ToolCallContent tc = new ToolCallContent("run_skill", result, "",
                    NodeStatus.FAIL, node, runId, id);
            if (config != null) tc.withMeta(config.meta());
            wfm.writeContext(node, "tool", JsonObject.mapFrom(tc));
            wfm.nextFailInvoke(node, e);
        }

        /**
         * 取消：写取消上下文 + 走取消 next
         */
        private void cancel(WorkFlowManage wfm, RunSkillNode node, RunSkillNode.RunSkillConfig config, String runId) {
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

        @Override
        public Supplier<List<Node>> apply(WorkFlowManage wfm, RunSkillNode node) {
            String runId = (String) wfm.getParams().get("workflowRunId");
            RunSkillNodeData data = node.params;
            RunSkillConfig config = resolveConfig(data, wfm);
            int timeout = DEFAULT_TIMEOUT;

            if (node.getStatus() == NodeStatus.CANCELLED) {
                node.status = NodeStatus.CANCELLED;
                wfm.nextInvoke(node, wfm.nextCancelNodeSupplier());
                return null;
            }

            if (config == null || StringUtils.isEmpty(config.skillId())) {
                fail(wfm, node, config, runId, "技能ID为空", "技能ID为空", 1, new RuntimeException("技能ID为空"));
                return null;
            }

            // 查找技能目录
            Path skillsDir = wfm.getApplicationDirectory().resolve("skills");
            Path skillDir = skillsDir.resolve(config.skillId());

            if (!Files.exists(skillDir) || !Files.isDirectory(skillDir)) {
                fail(wfm, node, config, runId, "技能目录不存在: " + config.skillId(),
                        "请先执行 download_skills", 1, new RuntimeException("技能目录不存在"));
                return null;
            }

            // 确定执行命令
            String command = config.command();
            if (StringUtils.isEmpty(command)) {
                command = readDefaultScript(skillDir);
            }
            if (StringUtils.isEmpty(command)) {
                fail(wfm, node, config, runId, "未指定执行命令", "未指定执行命令", 1, new RuntimeException("未指定执行命令"));
                return null;
            }

            if (config.withWriteArguments()) {
                wfm.write(node, new ToolCallContent("run_skill", "",
                        config.toArguments(), NodeStatus.RUNNING, node, runId, config.id()));
            }

            // 异步查询环境变量 → 创建 runner → 执行
            final String finalCommand = command;
            querySkillEnv(config.skillId(), wfm.getWorkingDirectory())
                    .onSuccess(env -> {
                        if (node.getStatus() == NodeStatus.CANCELLED) {
                            wfm.nextInvoke(node, wfm.nextCancelNodeSupplier());
                            return;
                        }

                        CommandRunner runner;
                        if ("docker".equals(config.runtime())) {
                            runner = new DockerCommandRunner(finalCommand, env, skillDir.toString(), timeout);
                        } else {
                            runner = new LocalCommandRunner(finalCommand, env, skillDir, timeout);
                        }
                        node.runner = runner;

                        runner.run(new CommandRunner.Listener() {
                            @Override
                            public void onNext(String chunk) {
                                wfm.write(node, new ToolCallContent("run_skill", chunk, "",
                                        NodeStatus.RUNNING, node, runId, config.id()));
                            }

                            @Override
                            public void onComplete(CommandResult result) {
                                if (node.getStatus() == NodeStatus.CANCELLED) {
                                    cancel(wfm, node, config, runId);
                                    return;
                                }
                                complete(wfm, node, config, runId, result.stdout(), result.stderr(), result.exitCode());
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (node.getStatus() == NodeStatus.CANCELLED) {
                                    cancel(wfm, node, config, runId);
                                    return;
                                }
                                fail(wfm, node, config, runId, e.getMessage(), e.getMessage(), 1, e);
                            }
                        });
                    })
                    .onFailure(e -> fail(wfm, node, config, runId,
                            "查询技能参数失败: " + e.getMessage(), e.getMessage(), 1, e));

            return null;
        }

        /**
         * 从数据库查询 skill 的 parameterValue，解密为环境变量
         */
        private Future<Map<String, String>> querySkillEnv(String skillId, Path workingDirectory) {
            SkillMapper skillMapper = RunApplication.appComponent.skillMapper();
            return skillMapper.getById(skillId)
                    .map(skill -> {
                        if (skill == null) return new HashMap<>();
                        JsonObject params = skill.decrypt();
                        if (params == null || params.isEmpty()) return new HashMap<>();
                        Map<String, String> env = new HashMap<>();
                        for (String key : params.fieldNames()) {
                            Object value = params.getValue(key);
                            if (value != null) {
                                env.put(key, value.toString());
                            }
                        }
                        env.put("RUNIFY_OUTPUT_DIR", workingDirectory.toString());
                        return env;
                    });
        }

        private String readDefaultScript(Path skillDir) {
            Path metaFile = skillDir.resolve(".skill-meta.json");
            if (!Files.exists(metaFile)) return null;
            try {
                String json = Files.readString(metaFile, StandardCharsets.UTF_8);
                JsonObject meta = new JsonObject(json);
                return meta.getString("defaultScript");
            } catch (Exception ignored) {
                return null;
            }
        }

        private RunSkillConfig resolveConfig(RunSkillNodeData data, WorkFlowManage wfm) {
            String id = CommonUtils.uuid7().toString();
            ToolCallMeta meta = ToolCallMeta.EMPTY;

            if ("tool_call".equals(data.getLocation())) {
                if (data.getReference() == null || data.getReference().isEmpty()) return null;
                Object ref = wfm.getContextVariable(data.getReference());
                if (ref instanceof JsonObject jo) {
                    meta = ToolCallMeta.from(jo);
                    if (jo.containsKey("id")) id = jo.getString("id");
                    String args = jo.getString("functionArguments");
                    if (!StringUtils.isEmpty(args)) {
                        JsonObject parsed = new JsonObject(args);
                        return new RunSkillConfig(id, parsed.getString("skill_id"),
                                parsed.getString("command"), null, false, meta);
                    }
                }
                return new RunSkillConfig(id, null, null, null, false, meta);
            }

            String skillId = resolveValue(data.getSkillIdLocation(), data.getSkillIdReference(), data.getSkillId(), wfm);
            String command = resolveValue(data.getCommandLocation(), data.getCommandReference(), data.getCommand(), wfm);
            String runtime = data.getRuntime();
            return new RunSkillConfig(id, skillId, command, runtime, true, ToolCallMeta.EMPTY);
        }

        private String resolveValue(String location, List<String> reference, String customValue, WorkFlowManage wfm) {
            if ("reference".equals(location)) {
                if (reference != null && !reference.isEmpty()) {
                    Object val = wfm.getContextVariable(reference);
                    if (val instanceof JsonObject jo) return jo.getString("command");
                    if (val instanceof String v) return v;
                    return val != null ? val.toString() : null;
                }
                return null;
            }
            return customValue;
        }
    }

    @Override
    public RunSkillNodeData getNodeData(JsonObject params) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData");
        RunSkillNodeData data = new RunSkillNodeData();
        data.setLocation(jsonObject.getString("location"));
        if (jsonObject.getJsonArray("reference") != null) {
            data.setReference(jsonObject.getJsonArray("reference").stream().map(Object::toString).toList());
        }
        data.setSkillIdLocation(jsonObject.getString("skillIdLocation"));
        if (jsonObject.getJsonArray("skillIdReference") != null) {
            data.setSkillIdReference(jsonObject.getJsonArray("skillIdReference").stream().map(Object::toString).toList());
        }
        data.setSkillId(jsonObject.getString("skillId"));
        data.setCommandLocation(jsonObject.getString("commandLocation"));
        if (jsonObject.getJsonArray("commandReference") != null) {
            data.setCommandReference(jsonObject.getJsonArray("commandReference").stream().map(Object::toString).toList());
        }
        data.setCommand(jsonObject.getString("command"));
        data.setRuntime(jsonObject.getString("runtime"));
        return data;
    }

    @Override
    public NodeResult<RunSkillNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
