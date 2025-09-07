package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jooq.SQLDialect;

import java.time.LocalDateTime;
import java.util.Map;
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
     * 节点类型
     */
    @Column(name = "type")
    private String type;

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

    public Knowledge(Row row) {
        this.id = row.getUUID("id");
        this.parentId = row.getUUID("parent_id");
        this.type = row.getString("type");
        this.meta = row.getJsonObject("meta");
        this.name = row.getString("name");
        this.content = row.getString("content");
        this.star = row.getBoolean("star");
        this.share = row.getBoolean("share");
        this.excerpt = row.getString("excerpt");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }


    @Override
    @JsonIgnore
    public Map<SQLDialect, BaseConvert<Knowledge>> getConvertMap() {
        return Map.of(SQLDialect.SQLITE, new Sqlite(),
                SQLDialect.POSTGRES, new Pgsql(),
                SQLDialect.H2, new Pgsql());
    }


    class Pgsql implements BaseConvert<Knowledge> {
        @Override
        public Knowledge mapTo(Row row) {
            return new Knowledge(row);
        }
    }

    class Sqlite implements BaseConvert<Knowledge> {


        @Override
        public Knowledge mapTo(Row row) {
            Knowledge knowledge = new Knowledge();
            knowledge.id = row.getUUID("id");
            knowledge.parentId = row.getUUID("parent_id");
            knowledge.type = row.getString("type");
            knowledge.meta = JacksonUtils.fromJson(row.getString("meta"), JsonObject.class);
            knowledge.name = row.getString("name");
            knowledge.content = row.getString("content");
            knowledge.star = row.getInteger("star") != 0;
            knowledge.share = row.getInteger("share") != 0;
            knowledge.excerpt = row.getString("excerpt");
            knowledge.createTime = row.getLocalDateTime("create_time");
            knowledge.updateTime = row.getLocalDateTime("update_time");
            return knowledge;
        }
    }


}
