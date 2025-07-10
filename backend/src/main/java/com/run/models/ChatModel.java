package com.run.models;

import com.run.common.openai.request.message.Message;
import com.run.common.openai.response.ChatCompletion;
import com.run.common.openai.response.ChatCompletionMessageToolCall;
import com.run.common.openai.response.CompletionUsage;
import com.run.common.openai.response.chunk.ChatCompletionChunk;
import com.run.models.callback.Callback;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * {@code @Author:张少虎}¬
 * {@code @Date: 2025/3/22  20:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ChatModel extends BaseModel {
    /**
     * 模型执行
     *
     * @param messages message
     * @param callback 回调
     * @param other    其他参数
     */
    void invoke(List<Message> messages,
                boolean stream,
                Callback<ChatCompletion, ChatCompletionChunk> callback,
                JsonObject other);

    /**
     * @return Tokens详情
     */
    CompletionUsage getCompletionUsage();

    /**
     * 模型停止生成
     * 如果模型达到自然停止点或提供的停止序列，则为 `stop`；如果达到请求中指定的最大 token 数量，则为 `length`；
     * 如果由于内容过滤器中的标志而省略内容，则为 `content_filter`；如果模型调用了工具，则为 `tool_calls`；
     * 如果模型调用了函数，则为 `function_call`（已弃用）。
     *
     * @return 模型停止生成原因
     */
    String getFinishReason();

    /**
     * 获取ToolCall
     *
     * @return 函数调用详情
     */
    List<ChatCompletionMessageToolCall> getChoiceDeltaToolCall();


}
