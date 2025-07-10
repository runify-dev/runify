package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class Choice extends JsonObject {
    /**
     * 模型停止生成令牌的原因。
     * 如果模型达到自然停止点或提供的停止点，这将是“停止”
     * sequence，如果请求中指定的最大令牌数为
     * 已达到，如果由于内容中的标志而省略了内容，则返回“content_filter”
     * 过滤器、`tool_calls`（如果模型调用了工具）或`function_call`（已弃用）如果模型调用了函数。
     * "stop", "length", "tool_calls", "content_filter", "function_call"
     */
    private String finish_reason;

    private Integer index;

    private ChoiceLogprobs logprobs;

    private ChatCompletionMessage message;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
