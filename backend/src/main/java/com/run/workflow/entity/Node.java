package com.run.workflow.entity;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  20:37}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@ToString
@Getter
@Setter
public class Node extends JsonObject {
    private String id;
    private String type;
    private Integer x;
    private Integer y;
    private JsonObject properties;

    @JsonAnySetter
    @Override
    public JsonObject put(String key, Object value) {
        return super.put(key, value);
    }

    public Node self() {
        return this;
    }

    public static Node of(String id, String type, Integer x, Integer y, JsonObject properties) {
        Node entries = new Node();
        entries.id = id;
        entries.type = type;
        entries.x = x;
        entries.y = y;
        entries.properties = properties;
        return entries;

    }

}
