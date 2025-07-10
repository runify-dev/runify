package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.response.ChoiceLogprobs;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class Choice extends JsonObject {
    /**
     * 流模型响应生成的聊天完成增量。
     */
    private ChoiceDelta delta;
    /**
     * 模型停止生成 token 的原因。
     * <p>
     * 如果模型达到自然停止点或提供的停止序列，则为 `stop`；如果达到请求中指定的最大 token 数量，则为 `length`；
     * 如果由于内容过滤器中的标志而省略内容，则为 `content_filter`；如果模型调用了工具，则为 `tool_calls`；
     * 如果模型调用了函数，则为 `function_call`（已弃用）。
     */
    private String finish_reason;
    /**
     * 选择列表中选择的索引。
     */
    private int index;
    /**
     * 记录选择的概率信息。
     */
    private ChoiceLogprobs logprobs;


    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }
}