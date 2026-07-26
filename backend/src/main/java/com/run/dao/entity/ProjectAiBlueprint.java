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
 * L1 项目蓝图 & 跨会话长期记忆（1:1 project）。
 * memory 存约定 / 决策 / 技术栈 / 偏好等结构化记忆，会话 start 时注入模型上下文。
 */
@Table(schemaName = "public", name = "project_ai_blueprint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAiBlueprint implements BaseEntity<ProjectAiBlueprint> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "description")
    private String description;

    /**
     * 项目规范（markdown 文档）：统一响应信封、路径前缀、错误格式、命名、"接口必须有描述"等；
     * 外层 agent 单写、稳定，注入每个子代理的生成需求，全项目一致。区别于 memory（学到的软偏好）。
     */
    @Column(name = "conventions")
    private String conventions;

    /**
     * 结构化记忆：AI 跨会话学到的软偏好 / 决策
     */
    @Column(name = "memory")
    private JsonObject memory;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
