package com.run.workflow.deserialize.databasesearch;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class DatabaseSearchDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "database-search-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("result")) {
            JsonArray array = context.getJsonArray("result");
            result.put("result", array != null ? array.getList() : null);
        }
        return result;
    }
}
