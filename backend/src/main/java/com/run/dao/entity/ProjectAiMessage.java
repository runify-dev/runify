package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一消息流（append-only）：ownerType 区分外层会话 / 子任务，喂模型窗口从这里取。
 * (ownerType, ownerId, seq) 顺序唯一；payload 为单条 OpenAI 消息；compacted 标记原文是否已并入 summary。
 */
@Table(schemaName = "public", name = "project_ai_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAiMessage implements BaseEntity<ProjectAiMessage> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    /**
     * 'session' | 'task'
     */
    @Column(name = "owner_type")
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    /**
     * owner 内递增序号
     */
    @Column(name = "seq")
    private Long seq;

    /**
     * 单条 OpenAI 消息（role/content/tool_calls/tool_call_id/...）
     */
    @Column(name = "payload")
    private JsonObject payload;

    /**
     * 审计 / 压缩预算
     */
    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * 是否已被压缩进所属 summary（原文保留供审计 / 重算）
     */
    @Column(name = "compacted")
    private Boolean compacted;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
