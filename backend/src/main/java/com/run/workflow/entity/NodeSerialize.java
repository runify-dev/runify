package com.run.workflow.entity;

import lombok.Data;

import java.util.Map;

@Data
public class NodeSerialize {
    private NodeInfo nodeInfo;
    private Map<String, Object> context;

    public Map<String, Object> toMap() {
        return Map.of("nodeInfo", nodeInfo, "context", context);
    }
}
