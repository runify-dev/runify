package com.run.workflow.nodes.contextmanage.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * context-manage 节点配置。
 * <p>
 * 节点是纯变换器：所有状态（原始上下文 / 摘要 / 便签，产物 = subtype=artifact 的便签）都通过变量进出，
 * 运行时不做任何数据库读写；跨对话持久化（ctx_fact / ctx_summary）
 * 属于载入/固化阶段的平台逻辑。所有数值都有后端默认值。
 */
@Getter
@Setter
public class ContextManageNodeData {
    /**
     * 上下文·初始值（输入，必填）：待压缩的上下文，如 [查询节点, history] 或 [start-node, messages]。
     */
    private List<String> sourceSeedVariable;
    /**
     * 上下文·变化值（输出，可空）：节点把"压缩 + 拼接摘要/便签"后的最终 messages 写回此变量，
     * 供 ai-chat 直接引用。须≠初始值（否则下一轮回读压缩产物 → 二次压缩）。
     */
    private List<String> sourceVariable;
    /**
     * 摘要·初始值（只读，可空）：跨对话摘要种子，裸字符串或 {text, covered}。
     * 仅在摘要变化值为空（首轮）时生效，永不被写。
     */
    private List<String> summarySeedVariable;
    /**
     * 摘要·变化值（读 + 写回）：{text, covered, seedCovered}，跨轮滚动。
     * 必须指向跨迭代持久的父层变量（循环体子上下文每次迭代重建，节点自身输出跨迭代不保留）。
     */
    private List<String> summaryVariable;
    /**
     * 便签·初始值（只读，可空）：常驻便签种子，元素 {subtype, key, value}（产物 subtype=artifact）。
     * 仅在便签变化值为空（首轮）时生效，永不被写。
     */
    private List<String> factsSeedVariable;
    /**
     * 便签·变化值（读 + 写回）：便签列表，同键覆盖累积。须指向父层持久变量。
     */
    private List<String> factsVariable;
    /**
     * 上下文预算（token）
     */
    private Integer budget;
    /**
     * 高水位比例：超过 budget*highRatio 才开始压缩
     */
    private Double highRatio;
    /**
     * 低水位比例：压到 budget*lowRatio 即停
     */
    private Double lowRatio;
    /**
     * 保护区：最近 N 条 Content 不打标
     */
    private Integer keepRecentItems;
    /**
     * 距末尾 ≥ N 条的快照/结论降级为首行
     */
    private Integer stubAge;
    /**
     * 距末尾 ≥ N 条且很长的内容截头尾
     */
    private Integer truncAge;
    /**
     * 是否剥离 QUESTION 的多模态字段（按目标模型是否支持多模态决定）
     */
    private Boolean stripMultimodal;
    /**
     * LLM 摘要开关：开启且配了模型时，"规则压完仍超低水位"才调一次模型（阈值触发，非每轮）；
     * 失败回退抽取式。关闭则恒用抽取式 digest（零成本）
     */
    private Boolean enableSummarizer;
    /**
     * LLM 摘要使用的模型 id
     */
    private String summarizerModelId;
    /**
     * 摘要方法：fc（默认，工具调用 + tool_choice 强制，schema 保证结构）
     * | prompt（提示词 + 标签块，兼容无 FC 能力的模型）
     */
    private String summarizerMethod;
    /**
     * 启用的便签子区
     */
    private List<String> factSections;
    /**
     * 固定开销（system 提示词 + tools schema 的估算），计入水位分母
     */
    private Integer reservedTokens;
    /**
     * token 编码：cl100k | o200k
     */
    private String tokenEncoding;
}
