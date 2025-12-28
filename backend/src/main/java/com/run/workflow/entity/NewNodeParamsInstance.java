package com.run.workflow.entity;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  23:10}
 * {@code @Version 1.0}
 * {@code @注释: }
 */

import com.run.workflow.INode;
import com.run.workflow.WorkflowType;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


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
    private INode<?, ?> upNode;
    private WorkflowType workflowType;

    public static NewNodeParamsInstance of(Node node,
                                           JsonObject params,
                                           List<String> upNodeIdList,
                                           Validator validator,
                                           INode<?, ?> upNode, WorkflowType workflowType) {
        return of(node, params, upNodeIdList, "", new JsonObject(), validator, upNode, workflowType);
    }

    public static NewNodeParamsInstance of(Node node, JsonObject params, List<String> upNodeIdList, String slat, JsonObject context, Validator validator, INode<?, ?> upNode, WorkflowType workflowType) {
        NewNodeParamsInstance instance = new NewNodeParamsInstance();
        instance.node = node;
        instance.params = params;
        instance.upNodeIdList = upNodeIdList;
        instance.context = context;
        instance.validator = validator;
        instance.salt = slat;
        instance.upNode = upNode;
        instance.workflowType = workflowType;
        return instance;
    }
}
