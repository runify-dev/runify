package com.run.workflow.nodes.contextpush.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 上下文推送节点数据
 */
@Getter
@Setter
public class ContextPushNodeData {
    /**
     * 上下文项列表
     */
    private List<ContextPushItem> items;

    @Getter
    @Setter
    public static class ContextPushItem {
        /**
         * 目标变量路径
         */
        private List<String> variable;
        /**
         * 数据来源模式: reference 或 custom
         */
        private String mode;
        /**
         * 引用变量路径（mode=reference时）
         */
        private List<String> reference;
        /**
         * 自定义内容（mode=custom时）
         */
        private String content;
        /**
         * 角色: system, user, assistant, tool
         */
        private String role;
    }
}
