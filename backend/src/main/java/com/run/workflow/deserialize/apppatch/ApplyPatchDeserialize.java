package com.run.workflow.deserialize.apppatch;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ApplyPatchDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "apply-patch-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) {
            result.put("result", context.getBoolean("result"));
        }
        if (context.containsKey("stdout")) {
            result.put("stdout", context.getString("stdout"));
        }
        if (context.containsKey("stderr")) {
            result.put("stderr", context.getString("stderr"));
        }
        if (context.containsKey("tool")) {
            result.put("tool", context.getJsonObject("tool"));
        }
        return result;
    }
}
