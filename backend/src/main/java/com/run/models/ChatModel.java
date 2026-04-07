package com.run.models;

import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.run.common.openai.request.message.Message;
import com.run.common.openai.response.ChatCompletionMessageToolCall;
import com.run.common.openai.response.CompletionUsage;
import com.run.models.callback.Callback;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code @Author:张少虎}¬
 * {@code @Date: 2025/3/22  20:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ChatModel extends BaseModel {

    /**
     * 非流式调用
     *
     * @param messages message上下文
     * @param extra    额外参数
     * @return 响应
     */
    CompletableFuture<ChatCompletion> invoke(List<ChatCompletionMessageParam> messages, JsonObject extra);

    /**
     * 流式调用
     *
     * @param messages message上下文
     * @param extra    额外参数
     * @return 响应
     */
    AsyncStreamResponse<ChatCompletionChunk> stream(List<ChatCompletionMessageParam> messages, JsonObject extra);

}
