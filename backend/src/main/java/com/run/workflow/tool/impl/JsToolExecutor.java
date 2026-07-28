package com.run.workflow.tool.impl;

import com.run.common.safeapi.CommonAPI;
import com.run.common.util.ConvertValueUtil;
import com.run.common.util.JacksonUtils;
import com.run.dao.entity.Tool;
import com.run.workflow.tool.ToolExecutor;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

/**
 * JS 运行时工具执行器。
 * 复用 java-script-node/ToolNode 的 GraalVM(polyglot) 执行方式。
 * <p>
 * body 约定: {@code {mode: 'function'|'script', code, functionName, allowIO}}。
 * input/config 分别以两个顶层对象注入脚本环境: {@code input.*} / {@code config.*}。
 */
public class JsToolExecutor implements ToolExecutor {

    @Override
    public String runtime() {
        return "JS";
    }

    @Override
    public Future<Object> execute(Tool tool, JsonObject input, JsonObject config, JsonObject caller,
                                  java.util.function.Consumer<com.run.workflow.message.struct.Content> onChunk, Vertx vertx) {
        // JS 工具是单次求值、无流式 chunk，onChunk 不适用
        Promise<Object> promise = Promise.promise();
        // GraalVM 执行是同步阻塞的，放到 worker 线程避免阻塞事件循环
        vertx.executeBlocking(() -> runScript(tool, input, config, caller), false)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

    private Object runScript(Tool tool, JsonObject input, JsonObject config, JsonObject caller) {
        JsonObject body = tool.getBody() != null ? tool.getBody() : new JsonObject();
        String mode = body.getString("mode", "function");
        String code = body.getString("code", "");
        boolean ioEnabled = Boolean.TRUE.equals(body.getBoolean("allowIO"));

        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.EXPLICIT)
                .allowAllAccess(ioEnabled)
                .allowCreateProcess(ioEnabled)
                .build()) {
            context.getBindings("js").putMember("api", new CommonAPI());
            // 注入命名空间：input(调用参数) / config(配置) / caller(调用方上下文)
            putJson(context, "input", input != null ? input : new JsonObject());
            putJson(context, "config", config != null ? config : new JsonObject());
            putJson(context, "caller", caller != null ? caller : new JsonObject());

            if ("function".equals(mode)) {
                context.eval("js", code);
                String functionName = body.getString("functionName", "handler");
                Value func = context.getBindings("js").getMember(functionName);
                if (func == null || !func.canExecute()) {
                    throw new RuntimeException("未找到可执行函数: " + functionName);
                }
                Value result = func.execute(context.getBindings("js").getMember("input"));
                return ConvertValueUtil.convertValue(result);
            } else {
                Value result = context.eval("js", code);
                return ConvertValueUtil.convertValue(result);
            }
        }
    }

    /**
     * 把 JsonObject 作为 JS 对象注入顶层变量
     */
    private void putJson(Context context, String name, JsonObject value) {
        String json = JacksonUtils.toJson(value.getMap());
        Value jsObj = context.eval("js", "(" + json + ")");
        context.getBindings("js").putMember(name, jsObj);
    }
}
