package com.run.workflow.nodes.loop.entity;

import com.run.workflow.entity.Edge;
import com.run.workflow.entity.Node;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 循环节点数据
 * 对应前端 loop-node 的 nodeData
 */
@Getter
@Setter
public class LoopNodeData {

    /**
     * 循环类型：foreach / count / infinite
     */
    private String loopType = "foreach";

    /**
     * 指定次数循环的次数（loopType = count 时使用）
     */
    private Integer loopCount;

    /**
     * 数组循环的变量路径（loopType = foreach 时使用）
     * 例如 ["start-node", "items"]
     */
    private List<String> loopVariable = new ArrayList<>();

    /**
     * 用户自定义的循环变量列表
     */
    private List<LoopVariable> loopVariables = new ArrayList<>();

    /**
     * 循环体子画布的节点和边
     */
    private Children children;

    @Getter
    @Setter
    public static class LoopVariable {
        private String name;
        private String label;
        private String dataType;
        private Object defaultValue;
    }

    @Getter
    @Setter
    public static class Children {
        private List<Node> nodes = new ArrayList<>();
        private List<Edge> edges = new ArrayList<>();
    }
}
