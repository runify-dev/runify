package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.response.chunk.ChatCompletionTokenLogprob;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ChoiceLogprobs extends JsonObject {
    /**
     * 带有对数概率信息的消息内容标记列表。
     */
    private List<ChatCompletionTokenLogprob> content;
    /**
     * 带有对数概率信息的消息拒绝标记列表。
     */
    private List<ChatCompletionTokenLogprob> refusal;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}