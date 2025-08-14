package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:29}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class TopLogprob extends JsonObject {
    /**
     * 令牌。
     */
    private String token;
    /**
     * 表示令牌的 UTF-8 字节表示形式的整数列表。
     * <p>
     * 在字符由多个令牌表示且
     * 它们的字节表示形式必须组合起来才能生成正确的文本
     * 表示形式的情况下很有用。如果令牌没有字节表示形式，则可以为“null”。
     */
    private List<Integer> bytes;
    /**
     * 如果该标记位于最有可能的前 20 个标记中，则为该标记的对数概率。
     * <p>
     * 否则，使用值 `-9999.0` 表示该标记非常不可能。
     */
    private Float logprob;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("bytes", bytes);
        result.put("logprob", logprob);
        result.putAll(getMap());
        return result;
    }
}
