package com.run.workflow.deserialize.currentuser;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class CurrentUserDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "current-user-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        for (String key : new String[]{"authenticated", "user", "roles", "permissions"}) {
            if (context.containsKey(key)) {
                result.put(key, context.getValue(key));
            }
        }
        return result;
    }
}
