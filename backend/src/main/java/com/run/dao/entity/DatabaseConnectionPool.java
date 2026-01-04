package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.common.constants.DatabaseConnectionProtocolConstants;
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
 * {@code @Date: 2025/12/31  21:23}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Table(schemaName = "public", name = "database_connection_pool")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConnectionPool implements BaseEntity<DatabaseConnectionPool> {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 项目id
     */
    @Column(name = "project_id")
    private UUID projectId;
    /**
     * 名称
     */
    @Column(name = "name")
    private String name;
    /**
     * 描述
     */
    @Column(name = "desc")
    private String desc;

    /**
     * 协议
     */
    @Column(name = "protocol")
    private DatabaseConnectionProtocolConstants protocol;

    /**
     * 元数据
     */
    @Column(name = "meta")
    private JsonObject meta;
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
