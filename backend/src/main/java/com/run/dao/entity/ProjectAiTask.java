package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * L3 生成任务台账（1:N session）：台账 + 子 agent 压缩状态 + 画布草稿 + UI 时间线。
 * projectId 有意冗余（按项目直查台账不必 join session）；workflow 是中断半成品草稿，
 * 区别于已交付的 processor.workflow；result 存 {valid, summary, validation}。
 */
@Table(schemaName = "public", name = "project_ai_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAiTask implements BaseEntity<ProjectAiTask> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "project_id")
    private UUID projectId;

    /**
     * 关联处理器实体（可能被删，可空）
     */
    @Column(name = "processor_id")
    private UUID processorId;

    @Column(name = "requirement")
    private String requirement;

    /**
     * queued / running / success / invalid / error
     */
    @Column(name = "status")
    private String status;

    @Column(name = "summary")
    private String summary;

    @Column(name = "facts")
    private JsonObject facts;

    @Column(name = "window_from_seq")
    private Long windowFromSeq;

    /**
     * 画布中间态草稿（中断半成品，区别于 processor.workflow）
     */
    @Column(name = "workflow")
    private JsonObject workflow;

    /**
     * { valid, summary, validation }
     */
    @Column(name = "result")
    private JsonObject result;

    @Column(name = "timeline")
    private JsonArray timeline;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
