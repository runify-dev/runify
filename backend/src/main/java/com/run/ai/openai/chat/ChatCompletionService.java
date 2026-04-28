package com.run.ai.openai.chat;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/** Synchronous chat completions service. */
public final class ChatCompletionService {

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public ChatCompletionService(OkHttpClient httpClient, String baseUrl, String apiKey) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public ChatCompletion create(ChatCompletionCreateParams params) {
        Request request = ChatCompletionHttpSupport.buildRequest(baseUrl, apiKey, params, false);
        Call call = httpClient.newCall(request);
        try (Response response = call.execute()) {
            return ChatCompletion.fromJson(ChatCompletionHttpSupport.readBodyOrThrow(response));
        } catch (IOException e) {
            throw new com.run.ai.openai.OpenAiException("OpenAI request failed", e);
        }
    }
}
