package com.run.workflow.deserialize.cachequery;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class CacheQueryDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "cache-query-node".equals(type);
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
