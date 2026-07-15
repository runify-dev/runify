package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 上下文便签（派生层）：约定/环境/目标/待办等键值事实。
 * owner 按 (scope_type, scope_id) 泛化——顶层对话与各子 agent 状态天然隔离。
 * 唯一键 (scope_type, scope_id, application_id, subtype, fact_key)。
 */
@Table(schemaName = "public", name = "ctx_fact")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CtxFact implements BaseEntity<CtxFact> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    /**
     * conversation | application | subtask
     */
    @Column(name = "scope_type")
    private String scopeType;

    /**
     * conversation → conversationId；application → applicationId；user → userId；
     * subtask → workflowRunId:节点id
     */
    @Column(name = "scope_id")
    private String scopeId;

    /**
     * 便签所属应用：仅 user 档使用，与 scope_id(=userId) 一起做 per-app 隔离（同一用户跨应用不串）；
     * conversation/application 档为空。取代旧的 scope_id="applicationId:userId" 字符串编码。
     */
    @Column(name = "application_id")
    private String applicationId;

    /**
     * convention | env | goal | todo | ...
     */
    @Column(name = "subtype")
    private String subtype;

    @Column(name = "fact_key")
    private String factKey;

    @Column(name = "fact_value")
    private String factValue;

    /**
     * 观察期标记：升应用级时为 true，用户确认后转 false
     */
    @Column(name = "provisional")
    private Boolean provisional;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
