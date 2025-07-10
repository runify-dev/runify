package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:16}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class Annotation extends JsonObject {
    private String type = "url_citation";
    /**
     * 消息的注释（如适用），如使用
     * [网络搜索工具](https://platform.openai.com/docs/guides/tools-web-search?api-模式=聊天）。
     */
    private AnnotationURLCitation url_citation;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
