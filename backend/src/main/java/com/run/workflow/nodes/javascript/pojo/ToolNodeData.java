package com.run.workflow.nodes.javascript.pojo;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ToolNodeData {
    /**
     * 执行模式: script(脚本模式) / function(函数模式)
     */
    private String mode;
    /**
     * 代码来源: reference 或 customize
     */
    private String codeLocation;
    /**
     * 代码引用变量路径
     */
    private List<String> codeReference;
    /**
     * 自定义代码
     */
    private String code;
    private String functionName;
    private Boolean allowIO;
    private List<Parameter> parameters;

    @Data
    public static class Parameter {
        private String field;
        private String description;
        private String required;
        /**
         * reference
         * customize
         */
        private String location;
        private String type;
        private Object value;
    }
}
