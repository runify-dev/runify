package com.run.workflow.nodes.aichat.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

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
    /**
     * 上下文配置
     */
    private ContextConfig contextConfig;
    /**
     * 工具配置
     */
    private ToolsConfig tools;

    @Getter
    @Setter
    public static class ContextConfig {
        /**
         * 是否启用自定义上下文
         */
        private Boolean enableContext;
        /**
         * 上下文变量路径
         */
        private List<String> contextVariable;
        /**
         * 上下文轮次限制（0或null表示全部）
         */
        private Integer contextNumber;
    }

    @Getter
    @Setter
    public static class ToolsConfig {
        /**
         * 引用类型: reference 或 customize
         */
        private String location;
        /**
         * 引用变量路径
         */
        private List<String> reference;
        /**
         * 自定义工具列表
         */
        private List<Tool> tools;
    }

    @Getter
    @Setter
    public static class Tool {
        /**
         * 工具类型: function
         */
        private String type;
        /**
         * 函数定义
         */
        private Function function;
    }

    @Getter
    @Setter
    public static class Function {
        /**
         * 函数名称
         */
        private String name;
        /**
         * 函数描述
         */
        private String description;
        /**
         * 参数定义
         */
        private Map<String, Object> parameters;
    }
}
