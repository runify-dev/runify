package com.run.workflow.deserialize.variableassign;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class VariableAssignDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "variable-assign-node".equals(type);
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
