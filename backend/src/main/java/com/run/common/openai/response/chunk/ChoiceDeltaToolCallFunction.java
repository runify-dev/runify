package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.result.Result;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:30}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ChoiceDeltaToolCallFunction extends JsonObject {
    /**
     * 调用函数的参数，由JSON中的模型生成
     * 格式。请注意，该模型并不总是生成有效的JSON，并且可能
     * 函数模式未定义的幻觉参数。验证
     * 在调用函数之前，在代码中添加参数。
     */
    private String arguments;
    /**
     * 要调用的函数的名称。
     */
    private String name;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("arguments", arguments);
        result.put("name", name);
        result.putAll(getMap());
        return result;
    }
}
