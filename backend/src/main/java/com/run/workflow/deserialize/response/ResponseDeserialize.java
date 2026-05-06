package com.run.workflow.deserialize.response;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ResponseDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "response-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        return new HashMap<>();
    }
}
