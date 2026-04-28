package com.run.ai.openai.chat;

import com.run.ai.openai.AsyncStreamResponse;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Asynchronous chat completions service. */
public final class AsyncChatCompletionService {

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final Executor executor;

    public AsyncChatCompletionService(OkHttpClient httpClient, String baseUrl, String apiKey, Executor executor) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.executor = executor;
    }

    public CompletableFuture<ChatCompletion> create(ChatCompletionCreateParams params) {
        return CompletableFuture.supplyAsync(() -> new ChatCompletionService(httpClient, baseUrl, apiKey).create(params), executor);
    }

    public AsyncStreamResponse<ChatCompletionChunk> createStreaming(ChatCompletionCreateParams params) {
        Request request = ChatCompletionHttpSupport.buildRequest(baseUrl, apiKey, params, true);
        Call call = httpClient.newCall(request);
        return new AsyncStreamResponse<>(call, executor, ChatCompletionChunk::fromJson);
    }
}
