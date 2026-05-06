package com.run.workflow.deserialize.aichat;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class AIChatDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "ai-chat-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("content")) {
            result.put("content", context.getString("content"));
        }
        if (context.containsKey("reasoningContent")) {
            result.put("reasoningContent", context.getString("reasoningContent"));
        }
        if (context.containsKey("refusal")) {
            result.put("refusal", context.getString("refusal"));
        }
        if (context.containsKey("isRefusal")) {
            result.put("isRefusal", context.getBoolean("isRefusal"));
        }
        if (context.containsKey("toolCalls")) {
            JsonArray toolCalls = context.getJsonArray("toolCalls");
            result.put("toolCalls", toolCalls != null ? toolCalls.getList() : null);
        }
        if (context.containsKey("finishReason")) {
            result.put("finishReason", context.getString("finishReason"));
        }
        return result;
    }
}
