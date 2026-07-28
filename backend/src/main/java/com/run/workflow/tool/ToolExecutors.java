package com.run.workflow.tool;

import com.run.dao.entity.Tool;
import com.run.workflow.tool.impl.JsToolExecutor;
import com.run.workflow.tool.impl.WorkflowToolExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具执行器注册表 + 配置解析工具。
 */
public class ToolExecutors {

    private static final Map<String, ToolExecutor> EXECUTORS =
            List.of(new WorkflowToolExecutor(), new JsToolExecutor())
                    .stream()
                    .collect(Collectors.toMap(ToolExecutor::runtime, e -> e));

    private ToolExecutors() {
    }

    public static ToolExecutor of(String runtime) {
        ToolExecutor executor = EXECUTORS.get(runtime);
        if (executor == null) {
            throw new RuntimeException("不支持的工具运行时: " + runtime);
        }
        return executor;
    }

    /**
     * 解析最终 config：node 覆盖值(nodeConfig) → 工具默认值(toolConfig) → configSchema.defaultValue。
     * "有值"= 非 null 且非空字符串。
     *
     * @param configSchema 工具的 configSchema
     * @param toolConfig   工具级配置值(已解密)
     * @param nodeConfig   绑定层(run-tool 节点)覆盖值(已解密)
     */
    public static JsonObject resolveConfig(JsonArray configSchema, JsonObject toolConfig, JsonObject nodeConfig) {
        JsonObject result = new JsonObject();
        if (configSchema == null) {
            return result;
        }
        for (int i = 0; i < configSchema.size(); i++) {
            JsonObject field = configSchema.getJsonObject(i);
            String key = field.getString("field");
            if (key == null) continue;
            Object value = firstSet(
                    nodeConfig != null ? nodeConfig.getValue(key) : null,
                    toolConfig != null ? toolConfig.getValue(key) : null,
                    field.getValue("defaultValue"));
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static Object firstSet(Object... values) {
        for (Object v : values) {
            if (isSet(v)) {
                return v;
            }
        }
        return null;
    }

    private static boolean isSet(Object v) {
        return v != null && !(v instanceof String s && s.isEmpty());
    }
}
