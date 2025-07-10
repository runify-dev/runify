package com.run.workflow.entity;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  20:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@ToString
public class Edge extends JsonObject {
    private String id;
    private String type;
    private String sourceNodeId;
    private String targetNodeId;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

}
