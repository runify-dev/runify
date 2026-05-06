package com.run.workflow.deserialize.approval;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ApprovalDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "approval-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        Map<String, Object> result = new HashMap<>();
        if (context.containsKey("approved")) {
            result.put("approved", context.getBoolean("approved"));
        }
        return result;
    }
}
