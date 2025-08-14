package com.run.common.openai.response.chunk;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.run.common.openai.response.CompletionUsage;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/17  21:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class ChatCompletionChunk extends JsonObject {


    /**
     * 聊天完成的唯一标识符。每个块都有相同的ID。
     */
    private String id;
    /**
     * 聊天完成选项列表。
     * 如果'n'大于1，则可以包含多个元素。也可以为空
     * 对于最后一个块，如果你设置`stream_options:{“includeusage”：true}。
     */
    private List<Choice> choices;
    /**
     * 创建聊天完成时的Unix时间戳（秒）。
     * 每个块都有相同的时间戳。
     */
    private Integer created;
    /**
     * 模型
     */
    private String model;
    /**
     * 对象类型，始终为“chat.ChatCompletion.chunk”。
     */
    private String object = "chat.ChatCompletion.chunk";
    /**
     * 用于处理请求的服务层。
     * scale|default
     */
    private String service_tier;
    /**
     * 此指纹表示模型运行时使用的后端配置。
     * 可以与“seed”请求参数结合使用，以了解何时
     * 后端更改可能会影响确定性。
     */
    private String system_fingerprint;
    /**
     * 一个可选字段，仅在您设置时显示
     * `stream_options:{“include_use”：true}`在您的请求中。当存在时，它
     * 包含空值，但包含令牌使用情况的最后一个块除外
     * 整个请求的统计数据。
     */
    private CompletionUsage usage;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public static ChatCompletionChunk of(String message) {
        ChatCompletionChunk chatCompletionChunk = new ChatCompletionChunk();
        Choice choice = new Choice();
        ChoiceDelta choiceDelta = new ChoiceDelta();
        choiceDelta.setContent(message);
        choice.setDelta(choiceDelta);
        choice.setIndex(0);
        chatCompletionChunk.setChoices(List.of(choice));
        return chatCompletionChunk;
    }

    public Map<String, Object> toOpenAIMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", this.id);
        result.put("choices", choices.stream().map(Choice::toMap).toList());
        result.put("created", created);
        result.put("model", model);
        result.put("object", object);
        result.put("service_tier", service_tier);
        result.put("system_fingerprint", system_fingerprint);
        result.put("usage", usage.toMap());
        result.putAll(getMap());
        return result;
    }

    public List<Map<String, Object>> toAppMap() {
        return this.getChoices().stream().map(item -> {
            Map<String, Object> result = new HashMap<>();
            ChoiceDelta delta = item.getDelta();
            result.put("content", delta.getContent());
            result.putAll(delta.getMap());
            return result;
        }).toList();
    }


}