package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class PromptTokensDetails extends JsonObject {
    /**
     * 提示中存在的音频输入token数。
     */
    private Integer audio_tokens;
    /**
     * 提示中存在缓存的token数。
     */
    private Integer cached_tokens;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("audio_tokens", audio_tokens);
        result.put("cached_tokens", cached_tokens);
        result.putAll(getMap());
        return result;

    }
}
