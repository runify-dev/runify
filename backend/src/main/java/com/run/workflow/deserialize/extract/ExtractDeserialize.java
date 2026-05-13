package com.run.workflow.deserialize.extract;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ExtractDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "extract-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) {
            result.put("result", context.getBoolean("result"));
        }
        if (context.containsKey("extracted")) {
            result.put("extracted", context.getJsonObject("extracted"));
        }
        if (context.containsKey("errors")) {
            result.put("errors", context.getJsonArray("errors"));
        }
        return result;
    }
}
