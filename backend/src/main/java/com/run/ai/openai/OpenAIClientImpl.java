package com.run.ai.openai;

import com.run.ai.openai.chat.AsyncChatCompletionService;
import com.run.ai.openai.chat.ChatCompletionService;
import okhttp3.OkHttpClient;

import java.util.concurrent.Executor;

final class OpenAIClientImpl implements OpenAIClient {

    private final ChatResource chatResource;
    private final AsyncOpenAIClient asyncClient;

    OpenAIClientImpl(OkHttpClient httpClient, String baseUrl, String apiKey, Executor executor) {
        ChatCompletionService chatCompletionService = new ChatCompletionService(httpClient, baseUrl, apiKey);
        AsyncChatCompletionService asyncChatCompletionService = new AsyncChatCompletionService(httpClient, baseUrl, apiKey, executor);
        this.chatResource = () -> chatCompletionService;
        this.asyncClient = () -> () -> asyncChatCompletionService;
    }

    @Override
    public ChatResource chat() {
        return chatResource;
    }

    @Override
    public AsyncOpenAIClient async() {
        return asyncClient;
    }
}
