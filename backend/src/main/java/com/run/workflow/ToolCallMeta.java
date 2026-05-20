package com.run.workflow;

import com.run.common.util.CommonUtils;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * AI 工具调用的元数据（reasoning / aiId / reasoningKey）
 * 从 tool_call 的 JsonObject 中提取，传递给 ToolCallContent
 */
public record ToolCallMeta(String reasoning, String reasoningKey, String aiId) {

    public static final ToolCallMeta EMPTY = new ToolCallMeta("", "reasoning_content", CommonUtils.uuid7().toString());

    /**
     * 从 tool_call 的 JsonObject 中提取元数据
     * 支持 AccumulatedToolCall 序列化后的格式
     */
    public static ToolCallMeta from(JsonObject toolCall) {
        if (toolCall == null) return EMPTY;
        String aiId = toolCall.getString("aiId");
        String reasoningKey = toolCall.getString("reasoningKey");
        String reasoning = toolCall.getString("reasoning");
        return new ToolCallMeta(
                StringUtils.isEmpty(reasoning) ? "" : reasoning,
                StringUtils.isEmpty(reasoningKey) ? "reasoning_content" : reasoningKey,
                StringUtils.isEmpty(aiId) ? CommonUtils.uuid7().toString() : aiId
        );
    }
}
