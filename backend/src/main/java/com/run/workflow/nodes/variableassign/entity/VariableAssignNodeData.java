package com.run.workflow.nodes.variableassign.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 变量赋值节点数据
 */
@Getter
@Setter
public class VariableAssignNodeData {
    /**
     * 变量列表
     */
    private List<VariableItem> variables;

    @Getter
    @Setter
    public static class VariableItem {
        /**
         * 目标变量路径
         */
        private List<String> variable;
        /**
         * 值类型: reference 或 constant
         */
        private String type;
        /**
         * 引用变量路径（type=reference时）
         */
        private List<String> reference;
        /**
         * 数据类型: string, array, dict, number, boolean
         */
        private String dataType;
        /**
         * 常量值（type=constant时）
         */
        private Object value;
    }
}
