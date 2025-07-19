package com.run.dao.entity;

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
 * {@code @Date: 2025/7/13  20:22}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "workflow")
public class Workflow implements BaseEntity<Workflow> {

    /**
     * 工作流id
     */
    @Column(name = "id", primaryKey = true)
    private UUID id;

    /**
     * 工作流名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 工作流对象
     */
    @Column(name = "workflow")
    private JsonObject workflow;

    /**
     * 类型
     */
    @Column(name = "type")
    private String type;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Workflow(Row row) {
        this.id = row.getUUID("id");
        this.workflow = row.getJsonObject("workflow");
        this.type = row.getString("type");
        this.name = row.getString("name");
        this.createTime = row.getLocalDateTime("create_time");
        this.updateTime = row.getLocalDateTime("update_time");
    }


    @Override
    public Workflow mapTo(Row row) {
        return new Workflow(row);
    }
}
