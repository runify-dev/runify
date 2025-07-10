package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:19}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ChatCompletionAudio extends JsonObject {
    /**
     * 此音频响应的唯一标识符。
     */
    private String id;
    /**
     * 模型生成的Base64编码音频字节，格式为
     * 请求。
     */
    private String data;
    /**
     * 当此音频响应不再时的Unix时间戳（秒）
     * 可在服务器上访问，用于多回合对话。
     */
    private Integer expires_at;
    /**
     * 模型生成的音频转录。
     */
    private String transcript;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
