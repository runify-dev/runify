package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.request.message.FunctionCall;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  23:13}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ChatCompletionMessage extends JsonObject {
    private String content;
    /**
     * 模型生成的拒绝消息。
     */
    private String refusal;
    /**
     * ai回复为assistant
     */
    private String role = "assistant";
    /**
     * 消息的注释（如适用），如使用
     * [网络搜索工具](https://platform.openai.com/docs/guides/tools-web-search?api-模式=聊天）。
     */
    private List<Annotation> annotations;
    /**
     * 如果请求了音频输出模态，则此对象包含有关
     * 模型的音频响应。
     * [了解更多](https://platform.openai.com/docs/guides/audio)
     */
    private ChatCompletionAudio audio;
    /**
     * 弃用并替换为“tool_calls”。
     * 应调用的函数的名称和参数，由
     * 模型。
     */
    @Deprecated
    private FunctionCall function_call;
    /**
     * 模型生成的工具调用，如函数调用。
     */
    private List<ChatCompletionMessageToolCall> tool_calls;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
