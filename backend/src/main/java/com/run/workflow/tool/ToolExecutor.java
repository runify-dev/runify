package com.run.workflow.tool;

import com.run.dao.entity.Tool;
import com.run.workflow.message.struct.Content;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.function.Consumer;

/**
 * 工具执行器策略。
 * 按 runtime(WORKFLOW/JS/PY) 多态实现；消费方只依赖工具契约，不关心实现。
 * <p>
 * 约定两个命名空间：
 * <ul>
 *   <li>{@code input.*} —— 本次调用参数（= inputSchema，对 LLM 可见）</li>
 *   <li>{@code config.*} —— 静态配置(已解密，含 secret)，绝不对 LLM 暴露</li>
 * </ul>
 */
public interface ToolExecutor {

    /**
     * 支持的运行时（WORKFLOW / JS / PY）
     */
    String runtime();

    /**
     * 执行工具。
     *
     * @param tool   工具定义
     * @param input  调用参数（inputSchema 解析后的值）
     * @param config 已解密并按 node.config→tool.config→defaultValue 解析后的配置值
     * @param caller  调用方上下文（谁调用谁知道：type=chat/processor 等）
     * @param onChunk 工具内部工作流产出的 chunk 原封不动透传给调用方输出流（如流式响应回流会话）；可为 null
     * @param vertx   vertx 实例
     * @return 工具输出（按 outputSchema 绑定，多值为 JsonObject）
     */
    Future<Object> execute(Tool tool, JsonObject input, JsonObject config, JsonObject caller,
                           Consumer<Content> onChunk, Vertx vertx);
}
