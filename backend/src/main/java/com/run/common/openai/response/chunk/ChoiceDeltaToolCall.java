package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
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
public class ChoiceDeltaToolCall extends JsonObject {
    private int index;
    /**
     * 工具调用的ID。
     */
    private String id;
    /**
     * 调用工具详情对象
     */
    private ChoiceDeltaToolCallFunction function;
    /**
     * 工具的类型。目前只支持“函数”。
     */
    private String type = "function";

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("id", id);
        result.put("function", function.toMap());
        result.put("type", type);
        return result;

    }
}
