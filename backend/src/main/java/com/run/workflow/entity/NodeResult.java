package com.run.workflow.entity;

import com.run.workflow.INode;
import com.run.workflow.WorkFlowManage;
import lombok.Getter;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:36}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
public class NodeResult<iNode extends INode> {
    private BiFunction<WorkFlowManage, iNode, Supplier<List<Node>>> handle;
    private iNode node;

    public NodeResult(BiFunction<WorkFlowManage, iNode, Supplier<List<Node>>> handle, iNode node) {
        this.node = node;
        this.handle = handle;
    }

    public Supplier<List<Node>> handle(WorkFlowManage workFlowManage) {
        return handle.apply(workFlowManage, node);

    }
}
