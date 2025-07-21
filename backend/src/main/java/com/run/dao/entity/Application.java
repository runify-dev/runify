package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.constants.DatabaseType;
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

import java.time.LocalDateTime;
import java.util.Map;
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
     * 节点类型
     */
    @Column(name = "type")
    private String type;

    /**
     * 应用描述
     */
    @Column(name = "desc")
    private String desc;
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

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Application(Row row) {
        this.id = row.getUUID("id");
        this.name = row.getString("name");
        this.desc = row.getString("desc");
        this.workflow = row.getJsonObject("workflow");
        this.setting = row.getJsonObject("setting");
        this.star = row.getBoolean("star");
        this.share = row.getBoolean("share");
        this.type = row.getString("type");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }


    @Override
    @JsonIgnore
    public Map<DatabaseType, BaseConvert<Application>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite(),
                DatabaseType.POSTGRESQL, new Pgsql());
    }


    class Pgsql implements BaseConvert<Application> {
        @Override
        public Application mapTo(Row row) {
            return new Application(row);
        }
    }

    class Sqlite implements BaseConvert<Application> {

        @Override
        public Application mapTo(Row row) {
            return new Application(row);
        }
    }

}
