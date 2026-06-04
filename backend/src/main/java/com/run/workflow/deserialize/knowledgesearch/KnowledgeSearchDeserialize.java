package com.run.workflow.deserialize.knowledgesearch;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class KnowledgeSearchDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "knowledge-search-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) {
            result.put("result", context.getValue("result"));
        }
        if (context.containsKey("hits")) {
            result.put("hits", context.getValue("hits"));
        }
        if (context.containsKey("total")) {
            result.put("total", context.getValue("total"));
        }
        if (context.containsKey("topScore")) {
            result.put("topScore", context.getValue("topScore"));
        }
        return result;
    }
}
