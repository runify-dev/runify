package com.run.workflow.deserialize.listdir;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ListDirDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "list-dir-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("content")) {
            result.put("content", context.getString("content"));
        }
        if (context.containsKey("summary")) {
            result.put("summary", context.getString("summary"));
        }
        if (context.containsKey("files")) {
            result.put("files", context.getInteger("files"));
        }
        if (context.containsKey("dirs")) {
            result.put("dirs", context.getInteger("dirs"));
        }
        if (context.containsKey("tool")) {
            result.put("tool", context.getJsonObject("tool"));
        }
        return result;
    }
}
