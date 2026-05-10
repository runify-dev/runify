package com.run.workflow.deserialize.cachewrite;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class CacheWriteDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "cache-write-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("success")) {
            result.put("success", context.getBoolean("success"));
        }
        return result;
    }
}
