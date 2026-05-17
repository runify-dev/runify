package com.run.workflow.entity;

import io.vertx.core.json.JsonObject;
import lombok.Data;

import java.util.Map;

@Data
public class NodeSerialize {
    private NodeInfo nodeInfo;
    private JsonObject context;

    public Map<String, Object> toMap() {
        return Map.of("nodeInfo", nodeInfo, "context", context);
    }
}
