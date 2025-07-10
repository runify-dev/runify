package com.run.common.openai.request.message;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class MessageToolCall {
    /**
     * 工具id
     */
    private String id;
    /**
     * 模型调用的函数。
     * {
     *     "arguments":"{\"location\": \"北京\"}",
     *     "name":"get_weather"
     * }
     */
    private Function function;

    private final String type = "function";
}
