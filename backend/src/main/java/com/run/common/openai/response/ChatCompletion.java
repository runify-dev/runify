package com.run.common.openai.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.response.chunk.ChatCompletionChunk;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  22:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class ChatCompletion extends JsonObject {
    /**
     * 聊天完成的唯一标识符。
     */
    private String id;
    /**
     * 聊天完成选项列表。
     * 如果'n'大于1，则可以多于一个。
     */
    private List<Choice> choices;

    private Integer created;
    /**
     * 模型
     */
    private String model;

    private String object = "chat.ChatCompletion";

    private String service_tier;

    private String system_fingerprint;

    private CompletionUsage usage;


    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public ChatCompletionChunk toChunk() {
        ChatCompletionChunk chatCompletionChunk = new ChatCompletionChunk();
        chatCompletionChunk.setUsage(usage);
        return chatCompletionChunk;
    }

}
