package com.run.workflow.deserialize.runtool;

import com.run.workflow.deserialize.INodeDeserialize;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class RunToolDeserialize implements INodeDeserialize {

    @Override
    public boolean support(String type) {
        return "run-tool-node".equals(type);
    }

    @Override
    public Map<String, Object> deserialize(JsonObject context) {
        // 工具输出字段是动态的(按 outputSchema)，整体回填
        return new HashMap<>(context.getMap());
    }
}
