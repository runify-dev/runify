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
 * 跨对话摘要（派生层）：一个 scope 一行。
 * 只在"下次对话载入历史"时使用（历史太长时用摘要固化老段，载入 = 摘要 + covered_upto 之后的回合）；
 * 运行时不读不写、不用游标。
 */
@Table(schemaName = "public", name = "ctx_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CtxSummary implements BaseEntity<CtxSummary> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    /**
     * conversation | subtask
     */
    @Column(name = "scope_type")
    private String scopeType;

    @Column(name = "scope_id")
    private String scopeId;

    @Column(name = "summary_text")
    private String summaryText;

    /**
     * 回合级游标：create_time <= covered_upto 的 conversation_message 已被摘要覆盖
     */
    @Column(name = "covered_upto")
    private LocalDateTime coveredUpto;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
