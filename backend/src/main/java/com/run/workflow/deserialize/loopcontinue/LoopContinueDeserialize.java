package com.run.workflow.deserialize.loopcontinue;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class LoopContinueDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "loop-continue-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        return new HashMap<>();
    }
}
