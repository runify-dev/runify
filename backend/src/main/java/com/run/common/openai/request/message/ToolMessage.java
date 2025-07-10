package com.run.common.openai.request.message;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:17}
 * {@code @Version 1.0}
 * {@code @注释: 调用工具}
 */
public class ToolMessage extends Message {
    /**
     * 用户消息的内容。
     */
    private String content;
    /**
     * 信息作者的角色，在这种情况下是“系统”。
     */
    private final String role = "tool";
    /**
     * 参与者的可选名称。
     * 提供模型信息以区分同一参与者
     * 角色。
     */
    private String tool_call_id;
}
