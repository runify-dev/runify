package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.response.chunk.CompletionTokensDetails;
import com.run.common.openai.response.chunk.PromptTokensDetails;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class CompletionUsage extends JsonObject {
    /**
     * 生成的完成中的令牌数量。
     */
    private Integer completion_tokens;
    /**
     * 请求的令牌数量。
     */
    private Integer prompt_tokens;
    /**
     * “请求中使用的令牌总数（提示 + 完成）。
     */
    private Integer total_tokens;
    /**
     * 完成任务中使用的 token 明细
     */
    private CompletionTokensDetails completion_tokens_details;
    /**
     * 请求中使用的 token 明细
     */
    private PromptTokensDetails prompt_tokens_details;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("completion_tokens", completion_tokens);
        result.put("prompt_tokens", prompt_tokens);
        result.put("total_tokens", total_tokens);
        result.put("completion_tokens_details", completion_tokens_details.toMap());
        result.put("prompt_tokens_details", prompt_tokens_details.toMap());
        result.putAll(getMap());
        return result;
    }
}
