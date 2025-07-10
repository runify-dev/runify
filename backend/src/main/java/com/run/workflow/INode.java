package com.run.workflow;

import com.run.common.util.CommonUtils;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.List;
import java.util.Set;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  20:43}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class INode<T extends INode, NodeData> {
    /**
     * 节点数据
     */
    protected Node node;
    /**
     * 执行当前节点所需要的参数
     */
    protected NodeData params;
    /**
     * 校验器
     */
    protected Validator validator;
    /**
     * 节点状态
     */
    protected NodeStatus status;
    /**
     * 节点上下文
     */
    protected JsonObject context;
    /**
     * 运行时id
     * 用于区分节点被运行多次的唯一标识
     */
    private String real_node_id;

    private List<String> upNodeIdList;

    public INode(Node node, JsonObject params, List<String> upNodeIdList, String salt) {
        this.node = node;
        this.params = getNodeData(params);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.upNodeIdList = upNodeIdList;
        this.real_node_id = CommonUtils.getSHA256(String.join("", upNodeIdList) + salt);
        this.context = new JsonObject();
        this.status = NodeStatus.BEFORE_RUNNING;
    }

    public INode(Node node,
                 JsonObject params,
                 List<String> upNodeIdList,
                 String salt,
                 JsonObject context,
                 Validator validator) {
        this.node = node;
        this.params = getNodeData(params);
        this.validator = validator;
        this.real_node_id = CommonUtils.getSHA256(String.join("", upNodeIdList) + salt);
        this.context = context;
        this.status = NodeStatus.BEFORE_RUNNING;
        this.upNodeIdList = upNodeIdList;
    }

    public abstract NodeData getNodeData(JsonObject params);

    /**
     * 校验
     */
    public void validate() {
        Set<ConstraintViolation<NodeData>> validate = this.validator.validate(this.params);
        validate.stream().findFirst().ifPresent(v -> {
            throw new RuntimeException(v.getMessage());
        });
    }

    /**
     * 执行节点
     */
    public abstract NodeResult<T> _invoke();

    public NodeResult<T> invoke() {
        this.status = NodeStatus.RUNNING;
        // 校验参数
        this.validate();
        return _invoke();
    }

    public void end(NodeStatus status) {
        this.status = status;
    }

    public List<String> getUpNodeIdList() {
        return this.upNodeIdList;
    }
}
