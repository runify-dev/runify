package com.run.common.openai.request.completion_create_params;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ChatCompletionToolParam {
    private final String type = "function";

    private FunctionDefinition function;

    public static ChatCompletionToolParam of(FunctionDefinition function) {
        ChatCompletionToolParam chatCompletionToolParam = new ChatCompletionToolParam();
        chatCompletionToolParam.setFunction(function);
        return chatCompletionToolParam;
    }
}
