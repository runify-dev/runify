package com.run.workflow.deserialize.contextsave;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ContextSaveDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "context-save-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        for (String key : context.fieldNames()) {
            result.put(key, context.getValue(key));
        }
        return result;
    }
}
