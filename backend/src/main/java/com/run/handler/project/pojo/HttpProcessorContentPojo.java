package com.run.handler.project.pojo;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/25  22:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class HttpProcessorContentPojo {
    private String method;
    private String url;
    private List<Parameters> parameters;

    @Data
    public static class Parameters {
        private String field;
        private String description;
        private Boolean required;
        private Boolean location;
        private String type;
        private Boolean many;
    }
}
