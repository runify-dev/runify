package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.common.constants.ProcessorProtocolConstants;
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
 * {@code @Date: 2025/12/22  20:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Table(schemaName = "public", name = "processor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Processor implements BaseEntity<Processor> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

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
    private ProcessorProtocolConstants protocol;

    @Column(name = "activate")
    private Boolean activate;
    /**
     * 元数据
     */
    @Column(name = "meta")
    private JsonObject meta;
    @Column(name = "workflow")
    private JsonObject workflow;
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
