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
public class CompletionTokensDetails extends JsonObject {
    /**
     * 使用预测输出时，预测中出现在完成中的token数。
     */
    private Integer accepted_prediction_tokens;
    /**
     * 模型生成的音频输入token数。
     */
    private Integer audio_tokens;
    /**
     * 模型生成的用于推理的token数。
     */
    private Integer reasoning_tokens;
    /**
     * 使用预测输出时，预测中未出现在完成中的标记数。但是，与推理标记一样，出于计费、输出和上下文窗口限制的目的，这些标记仍计入总完成标记中。
     */
    private Integer rejected_prediction_tokens;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("accepted_prediction_tokens", accepted_prediction_tokens);
        result.put("audio_tokens", audio_tokens);
        result.put("reasoning_tokens", reasoning_tokens);
        result.put("rejected_prediction_tokens", rejected_prediction_tokens);
        result.putAll(getMap());
        return result;
    }
}