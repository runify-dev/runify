package com.run.workflow.deserialize.javascript;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ToolDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "java-script-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) {
            result.put("result", context.getValue("result"));
        }
        return result;
    }
}
