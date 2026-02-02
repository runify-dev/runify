package com.run.workflow.nodes.databasesearch.pojo;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class DatabaseSearchNodeData {
    private List<String> pool;
    private String template;
    private List<Parameter> parameters;

    @Data
    public static class Parameter {
        private String field;
        private String desc;
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
