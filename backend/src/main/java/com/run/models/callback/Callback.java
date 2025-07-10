package com.run.models.callback;

import okhttp3.Call;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/24  22:29}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface Callback<ChatCompletion, ChatCompletionChunk> {

    void onResponse(@NotNull Call call, @NotNull ChatCompletion chatCompletion);

    void onStream(@NotNull Call call, @NotNull ChatCompletionChunk chatCompletion);

    /**
     * 成功响应
     *
     * @param call 成功
     */
    void onFinish(@NotNull Call call);

    /**
     * 错误响应
     * @param call call
     * @param e 错误
     */
    void onFailure(@NotNull Call call, @NotNull IOException e);
}
