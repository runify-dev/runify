package com.run.workflow.nodes.extract.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExtractNodeData {
    /**
     * 源变量引用路径
     */
    private List<String> sourceReference;

    /**
     * 提取规则列表
     */
    private List<ExtractRule> rules;

    @Getter
    @Setter
    public static class ExtractRule {
        /**
         * 输出字段名（写入 context 的 key）
         */
        private String name;
        /**
         * 描述（供 AI 理解用途）
         */
        private String description;
        /**
         * JSONPath 表达式
         */
        private String path;
    }
}
