package com.run.ai.openai;

import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder entrypoint similar to official OpenAIOkHttpClient, but only implements chat completions.
 */
public final class OpenAIOkHttpClient {

    private OpenAIOkHttpClient() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private OkHttpClient httpClient;
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private Executor executor;

        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public OpenAIClient build() {
            OkHttpClient finalHttpClient = httpClient == null ? new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(60))
                    .writeTimeout(Duration.ofSeconds(30))
                    .build() : httpClient;
            Executor finalExecutor = executor == null ? defaultExecutor() : executor;
            return new OpenAIClientImpl(finalHttpClient, normalizeBaseUrl(baseUrl), apiKey, finalExecutor);
        }

        private static String normalizeBaseUrl(String baseUrl) {
            String value = Objects.requireNonNull(baseUrl, "baseUrl cannot be null").trim();
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }

        private static ExecutorService defaultExecutor() {
            AtomicInteger counter = new AtomicInteger();
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, "light-openai-stream-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            return Executors.newCachedThreadPool(factory);
        }
    }
}
