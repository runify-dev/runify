package com.run.common.openai;


import com.run.common.openai.request.completion_create_params.ChatCompletionToolParam;
import com.run.common.openai.request.message.Message;
import io.vertx.core.json.JsonObject;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:37}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

public class OpenAI {
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient client;

    public OpenAI(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.client = new OkHttpClient();
    }

    private Request buildRequest(List<? extends Message> messages, boolean stream, List<ChatCompletionToolParam> tools, String tool_choice, JsonObject other) {
        // 构建请求体（JSON格式）
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : other) {
            json.put(entry.getKey(), entry.getValue());
        }
        json.put("model", this.model);
        json.put("messages", messages);
        json.put("stream", stream);
        if (tools != null) {
            json.put("tools", tools);
        }
        if (tool_choice != null) {
            json.put("tool_choice", tool_choice);
        }


        RequestBody body = RequestBody.create(json.toString(), JSON);

        // 构建请求
        return new Request.Builder()
                .url(this.baseUrl)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

    }

    /**
     * 阻塞调用
     *
     * @param messages    上下文
     * @param stream      是否流式响应
     * @param tools       工具
     * @param tool_choice 工具执行添加
     * @param other       其他参数
     */
    public Response invokeBlock(List<? extends Message> messages, boolean stream, List<ChatCompletionToolParam> tools, String tool_choice, JsonObject other) throws IOException {
        Request request = buildRequest(messages, stream, tools, tool_choice, other);
        Call call = client.newCall(request);
        return call.execute();
    }

    /**
     * 非阻塞调用
     *
     * @param messages    上下文
     * @param stream      是否流式响应
     * @param tools       工具
     * @param tool_choice 工具执行添加
     * @param callback    回掉
     * @param other       其他参数
     */
    public void invoke(List<? extends Message> messages, boolean stream, List<ChatCompletionToolParam> tools, String tool_choice, ChatCallback callback, JsonObject other) {

        Request request = buildRequest(messages, stream, tools, tool_choice, other);
        // 异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 读取流式响应
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            BufferedSource source = responseBody.source();
                            while (!source.exhausted()) {
                                String line = responseBody.source().readUtf8Line();
                                if (StringUtils.isEmpty(line)) {
                                    continue;
                                }

                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                callback.onResponse(call, line);

                            }
                        }
                    }
                } else {

                    byte[] bytes = response.body().source().readByteArray();
                    String s = new String(bytes);
                    System.out.println("请求失败: " + response.code() + " - " + s);
                }
            }
        });
    }

}
