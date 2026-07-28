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
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 资源版本(发布历史)。application/processor/tool 共用一张表,
 * append-only：同一 resource 的最新 version 即当前生效版本。}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "resource_version")
public class ResourceVersion implements BaseEntity<ResourceVersion> {
    /**
     * 版本id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;

    /**
     * 资源类型：application | processor | tool
     */
    @Column(name = "resource_type")
    private String resourceType;

    /**
     * 资源id(多态,不建外键,删资源时应用层级联删)
     */
    @Column(name = "resource_id")
    private UUID resourceId;

    /**
     * 版本号：每个 resource 内单调递增,latest = 生效版本
     */
    @Column(name = "version")
    private Integer version;

    /**
     * 版本快照。application:{workflow} processor:{workflow,meta} tool:{整套契约}
     */
    @Column(name = "snapshot")
    private JsonObject snapshot;

    /**
     * 发布备注
     */
    @Column(name = "remark")
    private String remark;

    /**
     * 发布人
     */
    @Column(name = "create_user")
    private UUID createUser;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
