package com.run.workflow;

import com.github.f4b6a3.uuid.UuidCreator;
import com.run.common.util.CommonUtils;
import com.run.workflow.entity.Node;
import com.run.workflow.entity.NodeResult;
import io.vertx.core.json.JsonObject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    @Getter
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
    @Getter
    protected NodeStatus status;
    /**
     * 节点上下文
     */
    @Getter
    protected JsonObject context;
    /**
     * 运行时id
     * 用于区分节点被运行多次的唯一标识
     */
    @Getter
    private String real_node_id;

    private String displayId;

    private INode<?, ?> upNode;
    @Getter
    private List<String> upNodeIdList;

    public Boolean getNodeDisplaySingle(Node node) {
        JsonObject jsonObject = node.getProperties().getJsonObject("nodeData", new JsonObject());
        Boolean single = jsonObject.getBoolean("displaySingle");
        return single != null && single;
    }

    public String getDisplayId() {
        if (StringUtils.isEmpty(displayId)) {
            Boolean single = getNodeDisplaySingle(node);
            if (single != null && single) {
                UUID timeOrdered = UuidCreator.getTimeOrderedEpoch();
                this.displayId = timeOrdered.toString();
            } else {
                if (upNode == null) {
                    UUID timeOrdered = UuidCreator.getTimeOrderedEpoch();
                    this.displayId = timeOrdered.toString();
                } else {
                    Boolean upNodeSingle = getNodeDisplaySingle(upNode.node);
                    if (upNodeSingle) {
                        UUID timeOrdered = UuidCreator.getTimeOrderedEpoch();
                        this.displayId = timeOrdered.toString();
                    } else {
                        this.displayId = upNode.getDisplayId();
                    }
                }
            }
        }
        return displayId;
    }

    public INode(Node node, JsonObject params, List<String> upNodeIdList, String salt, INode<?, ?> upNode) {
        this.node = node;
        this.params = getNodeData(params);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.upNode = upNode;
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
                 Validator validator
            , INode<?, ?> upNode) {
        this.node = node;
        this.params = getNodeData(params);
        this.validator = validator;
        this.real_node_id = CommonUtils.getSHA256(String.join("", upNodeIdList) + salt);
        this.context = context;
        this.status = NodeStatus.BEFORE_RUNNING;
        this.upNodeIdList = upNodeIdList;
        this.upNode = upNode;
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

}
