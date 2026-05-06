package com.run.workflow.deserialize.loopstart;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class LoopStartDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "loop-start-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("item")) {
            result.put("item", context.getValue("item"));
        }
        if (context.containsKey("index")) {
            result.put("index", context.getInteger("index"));
        }
        return result;
    }
}
