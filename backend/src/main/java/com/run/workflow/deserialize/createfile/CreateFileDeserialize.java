package com.run.workflow.deserialize.createfile;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class CreateFileDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "create-file-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) result.put("result", context.getString("result"));
        if (context.containsKey("tool")) result.put("tool", context.getJsonObject("tool"));
        return result;
    }
}
