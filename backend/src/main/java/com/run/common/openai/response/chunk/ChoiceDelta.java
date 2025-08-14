package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
@ToString
public class ChoiceDelta extends JsonObject {
    /**
     * 块消息的内容。
     */
    private String content;
    /**
     * 已弃用并由“tool_calls”取代。
     * <p>
     * 应调用的函数的名称和参数，由
     * 模型生成。
     */
    @Deprecated
    private ChoiceDeltaFunctionCall function_call;
    /**
     * 模型生成的拒绝消息。
     */
    private String refusal;
    /**
     * "developer", "system", "user", "assistant", "tool"
     */
    private String role;
    /**
     * 应调用的函数的名称和参数，由
     * 模型生成。
     */
    private List<ChoiceDeltaToolCall> tool_calls;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("content", this.content);
        result.put("function_call", function_call.toMap());
        result.put("refusal", this.refusal);
        result.put("role", this.role);
        result.put("tool_calls", this.tool_calls.stream().map(ChoiceDeltaToolCall::toMap).toList());
        result.putAll(getMap());
        return result;
    }
}