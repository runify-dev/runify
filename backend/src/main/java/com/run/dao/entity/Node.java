package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
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
@Table(schemaName = "public", name = "node")
public class Node implements BaseEntity<Node> {
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
     * 节点类型
     */
    @Column(name = "type")
    private String type;
    /**
     * 来源 KNOWLEDGE|APPLICATION
     */
    @Column(name = "source")
    private String source;
    /**
     * 子类型类型
     * 例如:
     */
    @Column(name = "subtype")
    private String subtype;
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

    public Node(Row row) {
        this.id = row.getUUID("id");
        this.parentId = row.getUUID("parent_id");
        this.type = row.getString("type");
        this.meta = row.getJsonObject("meta");
        this.name = row.getString("name");
        this.source = row.getString("source");
        this.subtype = row.getString("subtype");
        this.star = row.getBoolean("star");
        this.share = row.getBoolean("share");
        this.excerpt = row.getString("excerpt");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }


    @Override
    public Node mapTo(Row row) {
        return new Node(row);
    }
}
