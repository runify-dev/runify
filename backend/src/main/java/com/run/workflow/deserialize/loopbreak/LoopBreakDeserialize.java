package com.run.workflow.deserialize.loopbreak;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class LoopBreakDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "loop-break-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        return new HashMap<>();
    }
}
