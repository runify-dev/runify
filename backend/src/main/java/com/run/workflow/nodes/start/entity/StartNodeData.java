package com.run.workflow.nodes.start.entity;

import com.run.dao.entity.ConversationMessage;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/3/19  21:35}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class StartNodeData {
    @NotEmpty(message = "列表不能为空且至少包含一个元素")
    public List<ConversationMessage> messages;

    /**
     * 全局变量列表
     */
    private List<GlobalVariable> globalVariables;

    @Getter
    @Setter
    public static class GlobalVariable {
        /**
         * 变量名
         */
        private String name;
        /**
         * 显示名称
         */
        private String label;
        /**
         * 数据类型: string, number, boolean, array, dict
         */
        private String dataType;
        /**
         * 默认值
         */
        private Object defaultValue;
    }
}
