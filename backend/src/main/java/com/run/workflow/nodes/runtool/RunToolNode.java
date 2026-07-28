package com.run.workflow.nodes.runtool;

import com.run.RunApplication;
import com.run.dao.entity.Tool;
import com.run.dao.mapper.ToolMapper;
import com.run.workflow.INode;
import com.run.workflow.NodeStatus;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import com.run.workflow.message.struct.Position;
import com.run.workflow.nodes.runtool.entity.RunToolNodeData;
import com.run.workflow.tool.ToolExecutor;
import com.run.workflow.tool.ToolExecutors;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 调用工具节点。
 * 消费侧：应用/项目工作流通过它调用顶层"工具"资源(WORKFLOW/JS/PY 运行时对它透明)。
 */
public class RunToolNode extends INode<RunToolNode, RunToolNodeData> {

    public final static String type = "run-tool-node";
    public final static List<WorkflowType> supportWorkflow = List.of(
            WorkflowType.CHAT_WORKFLOW,
            WorkflowType.CHAT_WORKFLOW_LOOP,
            WorkflowType.PROCESSOR_HTTP,
            WorkflowType.PROCESSOR_HTTP_LOOP
    );

    public RunToolNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, upNode);
    }

    public RunToolNode(Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator, INode<?, ?> upNode) {
        super(node, params, upNodeIdList, salt, context, validator, upNode);
    }

    public static class Handle implements BiFunction<WorkFlowManage, RunToolNode, Supplier<List<Node>>> {

        @Override
        @SuppressWarnings("unchecked")
        public Supplier<List<Node>> apply(WorkFlowManage wfm, RunToolNode node) {
            RunToolNodeData data = node.params;
            if (StringUtils.isEmpty(data.getToolId())) {
                node.status = NodeStatus.FAIL;
                wfm.nextFailInvoke(node, new RuntimeException("未选择工具"));
                return null;
            }

            ToolMapper toolMapper = RunApplication.appComponent.toolMapper();
            Vertx vertx = RunApplication.appComponent.vertx();

            toolMapper.getById(data.getToolId())
                    .compose(tool -> {
                        if (tool == null) {
                            return Future.failedFuture(new RuntimeException("工具不存在"));
                        }
                        JsonObject input = resolveInputs(data, wfm);
                        JsonObject nodeConfig = data.getConfig() != null ? new JsonObject(data.getConfig()) : new JsonObject();
                        JsonObject config = ToolExecutors.resolveConfig(tool.getConfigSchema(), tool.decryptConfig(), nodeConfig);
                        JsonObject caller = buildCaller(wfm);
                        ToolExecutor executor = ToolExecutors.of(tool.getRuntime());
                        // 工具内部 chunk 透传到当前(调用方)输出流：内容原封不动，
                        // 但 position 重新归属到本 run-tool 节点(工具是黑盒，调用方不认识其内部节点)
                        return executor.execute(tool, input, config, caller, chunk -> {
                            chunk.setPosition(new Position(node.node.getId()));
                            wfm.write(node, chunk);
                        }, vertx);
                    })
                    .onSuccess(result -> {
                        node.status = NodeStatus.SUCCESS;
                        bindOutput(wfm, node, result);
                        wfm.nextInvoke(node, wfm.nextNodeSupplier(node.node.getId()));
                    })
                    .onFailure(e -> {
                        node.status = NodeStatus.FAIL;
                        wfm.writeContext(node, "result", e.getMessage());
                        wfm.nextFailInvoke(node, e);
                    });
            return null;
        }

        /**
         * 构造调用方上下文（谁调用谁知道）：只带调用方类型，供工具按 start-node.caller.* 分支。
         * 不带 应用/会话/用户 id —— 那是调用方私有信息，工具无需也不该知道。
         */
        private JsonObject buildCaller(WorkFlowManage wfm) {
            JsonObject caller = new JsonObject();
            WorkflowType type = wfm.getWorkFlow().getWorkflowType();
            String name = type != null ? type.name() : "";
            // 只对外给稳定的语义类型；不暴露内部枚举名(含 loop 变体)
            caller.put("type", name.startsWith("CHAT") ? "chat"
                    : name.startsWith("PROCESSOR") ? "processor" : "unknown");
            return caller;
        }

        /**
         * 解析入参映射为 input.* 对象
         */
        @SuppressWarnings("unchecked")
        private JsonObject resolveInputs(RunToolNodeData data, WorkFlowManage wfm) {
            JsonObject input = new JsonObject();
            if (data.getInputs() == null) {
                return input;
            }
            for (RunToolNodeData.InputBinding binding : data.getInputs()) {
                if (StringUtils.isEmpty(binding.getField())) continue;
                Object value;
                if ("reference".equals(binding.getLocation())) {
                    List<String> ref = toStringList(binding.getValue());
                    value = ref != null ? wfm.getContextVariable(ref) : null;
                } else {
                    value = binding.getValue();
                }
                input.put(binding.getField(), value);
            }
            return input;
        }

        private List<String> toStringList(Object value) {
            if (value instanceof JsonArray ja) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < ja.size(); i++) list.add(String.valueOf(ja.getValue(i)));
                return list;
            }
            if (value instanceof List<?> l) {
                List<String> list = new ArrayList<>();
                for (Object o : l) list.add(String.valueOf(o));
                return list;
            }
            return null;
        }

        /**
         * 输出绑定：多值(JsonObject)按 outputSchema 字段展开到上下文；同时整体写 result。
         */
        private void bindOutput(WorkFlowManage wfm, RunToolNode node, Object result) {
            if (result instanceof JsonObject jo) {
                for (Map.Entry<String, Object> entry : jo) {
                    wfm.writeContext(node, entry.getKey(), entry.getValue());
                }
            } else if (result instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> entry : m.entrySet()) {
                    wfm.writeContext(node, String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            wfm.writeContext(node, "result", result);
        }
    }

    @Override
    public RunToolNodeData getNodeData(JsonObject params) {
        JsonObject nd = node.getProperties().getJsonObject("nodeData", new JsonObject());
        RunToolNodeData data = new RunToolNodeData();
        data.setToolId(nd.getString("toolId"));

        JsonArray inputsArr = nd.getJsonArray("inputs");
        if (inputsArr != null) {
            List<RunToolNodeData.InputBinding> inputs = new ArrayList<>();
            for (int i = 0; i < inputsArr.size(); i++) {
                JsonObject item = inputsArr.getJsonObject(i);
                RunToolNodeData.InputBinding binding = new RunToolNodeData.InputBinding();
                binding.setField(item.getString("field"));
                binding.setLocation(item.getString("location"));
                binding.setValue(item.getValue("value"));
                inputs.add(binding);
            }
            data.setInputs(inputs);
        }

        JsonObject configObj = nd.getJsonObject("config");
        if (configObj != null) {
            data.setConfig(configObj.getMap());
        }
        return data;
    }

    @Override
    public NodeResult<RunToolNode> _invoke() {
        return new NodeResult<>(new Handle(), this);
    }
}
