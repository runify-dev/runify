package com.run.common.openai.request.message;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class FunctionMessage implements Message {

    /**
     * 用户消息的内容。
     */
    private String content;
    /**
     * 信息作者的角色，在这种情况下是“系统”。
     */
    private final String role = "function";
    /**
     * 要调用的函数的名称。
     */
    private String name;
}
