package com.run.common.openai.request.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:25}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class AssistantMessage implements Message {
    /**
     * 信息作者的角色，在这种情况下是“助手”。
     */
    private final String role = "assistant";

    private Audio audio;

    private String content;
    /**
     * 弃用并替换为“tool_calls”。
     * 应调用的函数的名称和参数，由
     * 模型。
     */
    private FunctionCall function_call;
    /**
     * 参与者的可选名称。
     * 提供模型信息以区分同一参与者
     * 角色。
     */
    private String name;
    /**
     * 助理的拒绝信息。
     */
    private String refusal;
    /**
     * 模型生成的工具调用，如函数调用。
     */
    private List<MessageToolCall> tool_calls;
}
