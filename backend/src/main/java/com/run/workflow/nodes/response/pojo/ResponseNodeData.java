package com.run.workflow.nodes.response.pojo;

import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/28  22:00}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ResponseNodeData {
    /**
     * 状态码
     */
    private Integer status;
    private List<JsonField> jsonFields;
    private JsonObject jsonObject;
    private PlainText plainText;
    private List<Header> headers;
    /**
     * jsonFields
     * jsonObject
     * plainText
     */
    private ContentType contentType;

    public enum ContentType {
        jsonFields,
        jsonObject,
        plainText
    }

    @Data
    public static class Header {
        private String field;
        private String location;
        private List<String> reference;
        private String value;
    }

    @Data
    public static class JsonField {
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
        private List<String> reference;
    }

    @Data
    public static class JsonObject {
        /**
         * reference
         * customize
         */
        private String location;
        private String value;
        private List<String> reference;
    }

    @Data
    public static class PlainText {
        /**
         * reference
         * customize
         */
        private String location;
        private String value;
        private List<String> reference;
    }

}
