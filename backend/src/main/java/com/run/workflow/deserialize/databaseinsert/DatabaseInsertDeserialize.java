package com.run.workflow.deserialize.databaseinsert;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class DatabaseInsertDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "database-insert-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("affectedRows")) {
            result.put("affectedRows", context.getInteger("affectedRows"));
        }
        return result;
    }
}
