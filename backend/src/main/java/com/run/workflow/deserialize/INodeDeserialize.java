package com.run.workflow.deserialize;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface INodeDeserialize {
    boolean support(String type);

    Map<String, Object> deserialize(JsonObject context);
}
