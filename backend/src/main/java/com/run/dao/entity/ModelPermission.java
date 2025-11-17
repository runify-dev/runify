package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/23  22:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Table(schemaName = "public", name = "knowledge_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModelPermission implements BaseEntity<ModelPermission> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "target")
    private UUID target;

    @Column(name = "permission")
    private String permission;
    /**
     * 修改时间
     */
    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
