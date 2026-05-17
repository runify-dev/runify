package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.auth.constants.TokenTypeConstants;
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
 * {@code @Date: 2025/7/13  20:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "application")
public class Application implements BaseEntity<Application> {
    /**
     * 应用id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 父id
     */
    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * 应用名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 应用描述
     */
    @Column(name = "desc")
    private String desc;
    /**
     * icon
     */
    @Column(name = "icon")
    private String icon;
    /**
     * 工作流对象
     */
    @Column(name = "workflow")
    private JsonObject workflow;
    /**
     * 应用设置
     */
    @Column(name = "setting")
    private JsonObject setting;
    /**
     * 加星
     */
    @Column(name = "star")
    private Boolean star;
    /**
     * 分享
     */
    @Column(name = "share")
    private Boolean share;
    /**
     * 是否开启匿名访问
     */
    @Column(name = "allow_anonymous_access")
    private Boolean allowAnonymousAccess;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
