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
 * L2 会话（1:N project）：元数据 + 压缩状态 + UI 时间线；消息正文见 {@link ProjectAiMessage}。
 * summary/facts/windowFromSeq 服务上下文压缩（M5），timeline 供整读整画恢复对话面板。
 */
@Table(schemaName = "public", name = "project_ai_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAiSession implements BaseEntity<ProjectAiSession> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "title")
    private String title;

    /**
     * running / awaiting / paused / done / error / stopped
     */
    @Column(name = "status")
    private String status;

    /**
     * 压缩摘要（喂模型窗口的一部分）
     */
    @Column(name = "summary")
    private String summary;

    /**
     * 会话级结构化便签（压缩产物）
     */
    @Column(name = "facts")
    private JsonObject facts;

    /**
     * 保留边界：此 seq 起取原文，之前已并入 summary
     */
    @Column(name = "window_from_seq")
    private Long windowFromSeq;

    /**
     * UI 事件（整读整画，恢复对话面板）
     */
    @Column(name = "timeline")
    private JsonArray timeline;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
