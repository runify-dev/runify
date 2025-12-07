package com.run.common.openai.request.message;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:20}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class UserMessage implements Message {
    /**
     * 用户消息的内容。
     */
    private String content;
    /**
     * 信息作者的角色，在这种情况下是“系统”。
     */
    private final String role = "user";
    /**
     * 参与者的可选名称。
     * 提供模型信息以区分同一参与者
     * 角色。
     */
    private String name;

    public UserMessage(String content) {
        this.content = content;
    }

    public Map<String, Object> toMap() {
        return Map.of("role", this.role, "content", this.content);
    }
}
