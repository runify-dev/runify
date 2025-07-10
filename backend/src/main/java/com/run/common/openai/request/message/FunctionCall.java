package com.run.common.openai.request.message;

import lombok.Getter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public class FunctionCall {
    /**
     * 调用函数的参数，由JSON中的模型生成
     * 格式。请注意，该模型并不总是生成有效的JSON，并且可能
     * 函数模式未定义的幻觉参数。验证
     * 在调用函数之前，在代码中添加参数。
     * arguments='{"location": "北京"}'
     */
    private String arguments;
    /**
     * 要调用的函数的名称。
     * get_weather
     */
    private String name;
}
