package com.run.models;


import com.run.ai.openai.AsyncStreamResponse;
import com.run.ai.openai.JsonValue;
import com.run.ai.openai.OpenAIClient;
import com.run.ai.openai.OpenAIOkHttpClient;
import com.run.ai.openai.chat.*;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/22  21:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class BaseOpenaiChatModel implements ChatModel {
    /**
     * url
     */
    protected String baseUrl;
    /**
     * key
     */
    protected String apiKey;
    /**
     * 模型名称
     */
    protected String model;
    /**
     * 调用客户端
     */
    private OpenAIClient client;

    public BaseOpenaiChatModel(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.client = OpenAIOkHttpClient
                .builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }


    @Override
    public void validate(String modelType, String modelName, Map<String, Object> modelCredential, Map<String, Object> other) {
        try {
            this.client.chat().completions().create(ChatCompletionCreateParams.builder()
                    .addMessage(ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content("你好").build()))
                    .model(model)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("模型参数错误" + e);
        }
    }

    @Override
    public CompletableFuture<ChatCompletion> invoke(List<ChatCompletionMessageParam> messages,
                                                    JsonObject extra) {
        return this.client.async().chat().completions().create(getChatCompletionCreateParams(messages, extra));
    }

    public ChatCompletionCreateParams getChatCompletionCreateParams(List<ChatCompletionMessageParam> messages,
                                                                    JsonObject extra) {
        ChatCompletionCreateParams.Builder params = ChatCompletionCreateParams.builder().messages(messages);
        params.model(model);
        for (Map.Entry<String, Object> args : extra) {
            String key = args.getKey();
            params.putAdditionalBodyProperty(key, JsonValue.from(args.getValue()));
        }
        return params.build();
    }

    @Override
    public AsyncStreamResponse<ChatCompletionChunk> stream(List<ChatCompletionMessageParam> messages, JsonObject extra) {
        return this.client.async().chat().completions().createStreaming(getChatCompletionCreateParams(messages, extra));
    }

}
