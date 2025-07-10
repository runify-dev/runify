package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class AnnotationURLCitation extends JsonObject {
    /**
     * 消息中URL引用的最后一个字符的索引。
     */
    private Integer end_index;
    /**
     * 消息中URL引用的开始字符的索引。
     */
    private Integer start_index;
    /**
     * web资源的标题。
     */
    private String title;
    /**
     * web资源的URL。
     */
    private String url;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
