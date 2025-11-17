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
 * {@code @Date: 2025/3/31  21:24}
 * {@code @Version 1.0}
 * {@code @注释: 知识库节点}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "knowledge")
public class Knowledge implements BaseEntity<Knowledge> {
    /**
     * 节点id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 父id
     */
    @Column(name = "parent_id")
    private UUID parentId;
    /**
     * 节点名称
     */
    @Column(name = "name")
    private String name;
    /**
     * icon
     */
    @Column(name = "icon")
    private String icon;

    /**
     * 知识库数据
     */
    @Column(name = "content")
    private String content;

    /**
     * 摘要
     */
    @Column(name = "excerpt")
    private String excerpt;
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
     * 元数据
     */
    @Column(name = "meta")
    private JsonObject meta;
    /**
     * 创建事件
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
