package com.run.workflow.deserialize.grep;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class GrepDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "grep-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("content")) result.put("content", context.getString("content"));
        if (context.containsKey("summary")) result.put("summary", context.getString("summary"));
        if (context.containsKey("matches")) result.put("matches", context.getInteger("matches"));
        if (context.containsKey("files")) result.put("files", context.getInteger("files"));
        if (context.containsKey("tool")) result.put("tool", context.getJsonObject("tool"));
        return result;
    }
}
