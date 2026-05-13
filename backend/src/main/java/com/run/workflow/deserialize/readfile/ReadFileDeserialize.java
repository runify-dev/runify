package com.run.workflow.deserialize.readfile;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ReadFileDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "read-file-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("content")) {
            result.put("content", context.getString("content"));
        }
        if (context.containsKey("rawContent")) {
            result.put("rawContent", context.getString("rawContent"));
        }
        if (context.containsKey("totalLines")) {
            result.put("totalLines", context.getInteger("totalLines"));
        }
        if (context.containsKey("lines")) {
            result.put("lines", context.getInteger("lines"));
        }
        if (context.containsKey("error")) {
            result.put("error", context.getString("error"));
        }
        if (context.containsKey("tool")) {
            result.put("tool", context.getJsonObject("tool"));
        }
        return result;
    }
}
