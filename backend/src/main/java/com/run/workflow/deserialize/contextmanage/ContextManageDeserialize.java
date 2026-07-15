package com.run.workflow.deserialize.contextmanage;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ContextManageDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "context-manage-node".equals(type);
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
