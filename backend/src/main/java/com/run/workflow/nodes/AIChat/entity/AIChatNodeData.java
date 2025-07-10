package com.run.workflow.nodes.AIChat.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/21  22:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class AIChatNodeData {
    /**
     * 模型id
     */
    private String modelId;
    /**
     * 系统提示词
     */
    private String system;
    /**
     * 用户提示词
     */
    private String user;


}
