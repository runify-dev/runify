package com.run.workflow.nodes.start.entity;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  13:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class HttpMeta {
    private String method;
    private String path;
    private List<Parameter> parameters;

    @Data
    public static class Parameter {
        private String field;
        private String description;
        private Boolean required;
        private String location;
        private String type;
        private Boolean many;
    }


}
