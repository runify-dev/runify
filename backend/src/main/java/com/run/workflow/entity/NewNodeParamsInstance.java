package com.run.workflow.entity;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  23:10}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Node node, JsonObject params, List<String> upNodeIdList, String salt, JsonObject context, Validator validator
 */
@Getter
@Setter
@ToString
public class NewNodeParamsInstance {
    private Node node;
    private JsonObject params;
    private List<String> upNodeIdList;
    private String salt;
    private JsonObject context;
    private Validator validator;

    public static NewNodeParamsInstance of(Node node, JsonObject params, List<String> upNodeIdList, Validator validator) {
        return of(node, params, upNodeIdList, "", new JsonObject(), validator);
    }

    public static NewNodeParamsInstance of(Node node, JsonObject params, List<String> upNodeIdList, String slat, JsonObject context, Validator validator) {
        NewNodeParamsInstance instance = new NewNodeParamsInstance();
        instance.node = node;
        instance.params = params;
        instance.upNodeIdList = upNodeIdList;
        instance.context = context;
        instance.validator = validator;
        instance.salt = slat;
        return instance;
    }
}
