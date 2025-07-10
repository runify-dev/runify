package com.run.common.openai;

import okhttp3.Call;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/22  19:19}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ChatCallback {

    void onFailure(@NotNull Call call, @NotNull IOException e);

    void onResponse(@NotNull Call call, @NotNull String chunk);
}
