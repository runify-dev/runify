package com.run.workflow.nodes.factextract.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * AI 便签提取节点配置。
 * <p>
 * 节点是纯变换器：读一段消息 → AI 抽便签 → 合并写回 facts 变量（同键覆盖、跨轮累积）。
 * 时机由画布位置决定（用户问题进来放一个、循环里放一个都行），节点自身不做触发判断。
 */
@Getter
@Setter
public class FactExtractNodeData {
    /**
     * 源消息变量（读）：要从中抽便签的消息列表，如 [start-node, messages] 或 [外循环, context]
     */
    private List<String> sourceReference;
    /**
     * 便签变量（读 + 写回）：抽到的便签同键覆盖累积进此变量，须指向跨迭代持久的父层变量
     */
    private List<String> factsVariable;
    /**
     * 提取模型 id
     */
    private String modelId;
    /**
     * 提取方法：fc（工具调用，默认）| prompt（标签块，兼容无 FC 能力模型）
     */
    private String method;
    /**
     * 启用的便签子区：convention/preference/env/goal/todo
     */
    private List<String> factSections;
}
