package com.run.workflow.tool.impl;

import com.run.common.util.CommonUtils;
import com.run.dao.entity.Tool;
import com.run.workflow.WorkFlowManage;
import com.run.workflow.WorkflowType;
import com.run.workflow.entity.WorkFlow;
import com.run.workflow.message.struct.Content;
import com.run.workflow.message.struct.FailureContent;
import com.run.workflow.message.struct.JsonFieldsContent;
import com.run.workflow.message.struct.TextContent;
import com.run.workflow.tool.ToolExecutor;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WORKFLOW 运行时工具执行器。
 * 起一个 {@link WorkflowType#TOOL} 的 {@link WorkFlowManage} 跑工具工作流。
 * <p>
 * 工具的返回 = outputSchema：画布用「变量赋值」写入 {@code output.<字段>}（专用「参数输出」命名空间），
 * 结束时从 {@code output} 上下文按 outputSchema 挑字段作为输出交还调用方。
 * （{@code global} 是自由暂存区，与对外输出隔离。）
 * （响应节点是调用方——智能体/处理器——的事，不是工具的输出机制。）
 * <p>
 * body 即工作流图 JSON({nodes, edges})。input/config 通过 params 注入，由 ToolStartNode 写进上下文。
 * JsonFieldsContent/文本仅作兜底。
 */
public class WorkflowToolExecutor implements ToolExecutor {

    @Override
    public String runtime() {
        return "WORKFLOW";
    }

    @Override
    public Future<Object> execute(Tool tool, JsonObject input, JsonObject config, JsonObject caller,
                                  java.util.function.Consumer<Content> onChunk, Vertx vertx) {
        Promise<Object> promise = Promise.promise();
        WorkFlow workFlow = WorkFlow.of(tool.getBody(), WorkflowType.TOOL);
        String workflowRunId = CommonUtils.uuid7().toString();

        Map<String, Object> params = new HashMap<>();
        params.put("input", input != null ? input.getMap() : new HashMap<>());
        params.put("config", config != null ? config.getMap() : new HashMap<>());
        params.put("caller", caller != null ? caller.getMap() : new HashMap<>());
        params.put("workflowRunId", workflowRunId);

        // 兜底累积（响应节点 JsonFieldsContent / 文本）；工具输出主路是 outputSchema→global(变量赋值)
        Map<String, Object> fallbackFields = new LinkedHashMap<>();
        StringBuilder textBuffer = new StringBuilder();
        AtomicBoolean failed = new AtomicBoolean(false);

        WorkFlowManage workFlowManage = new WorkFlowManage(workFlow, params, new HashMap<>(),
                (wm, node, chunk, done) -> {
                    if (Boolean.TRUE.equals(done)) {
                        if (promise.future().isComplete()) {
                            return;
                        }
                        promise.complete(resolveOutput(tool, wm, fallbackFields, textBuffer));
                        return;
                    }
                    Content c = chunk;
                    if (c instanceof FailureContent failureContent) {
                        // 失败由 RunToolNode 的 fail 分支统一上报，不透传(避免调用方双重报错)
                        if (failed.compareAndSet(false, true) && !promise.future().isComplete()) {
                            promise.fail(new RuntimeException(failureContent.getContent()));
                        }
                        return;
                    }
                    // 其余 chunk 原封不动透传给调用方输出流（流式响应回流等）
                    if (onChunk != null && c != null) {
                        onChunk.accept(c);
                    }
                    if (c instanceof JsonFieldsContent fields && fields.getContent() != null) {
                        fallbackFields.putAll(fields.getContent());
                    } else if (c instanceof TextContent textContent && textContent.getContent() != null) {
                        textBuffer.append(textContent.getContent());
                    }
                });
        try {
            workFlowManage.invoke();
        } catch (Throwable e) {
            if (!promise.future().isComplete()) {
                promise.fail(e);
            }
        }
        return promise.future();
    }

    /**
     * 输出解析：outputSchema 主路走 output(变量赋值写「参数输出」命名空间) 按字段挑；响应节点字段/文本仅兜底。
     * （global 是自由暂存区，不参与对外输出。）
     */
    @SuppressWarnings("unchecked")
    private Object resolveOutput(Tool tool, WorkFlowManage wm,
                                 Map<String, Object> fallbackFields, StringBuilder textBuffer) {
        Object o = wm.getContext().get("output");
        Map<String, Object> output = o instanceof Map ? (Map<String, Object>) o : Map.of();

        JsonArray outputSchema = tool.getOutputSchema();
        Map<String, Object> result = new LinkedHashMap<>();
        if (outputSchema != null && !outputSchema.isEmpty()) {
            for (int i = 0; i < outputSchema.size(); i++) {
                JsonObject field = outputSchema.getJsonObject(i);
                String name = field.getString("field");
                if (name == null || name.isEmpty()) continue;
                Object value = output.containsKey(name) ? output.get(name) : fallbackFields.get(name);
                result.put(name, value);
            }
            return new JsonObject(result);
        }
        if (!output.isEmpty()) {
            return new JsonObject(new LinkedHashMap<>(output));
        }
        if (!fallbackFields.isEmpty()) {
            return new JsonObject(fallbackFields);
        }
        if (!textBuffer.isEmpty()) {
            return textBuffer.toString();
        }
        return new JsonObject();
    }
}
