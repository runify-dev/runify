package com.run.dao.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.constants.DatabaseType;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
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
 * {@code @Date: 2025/3/26  22:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "model")
public class Model implements BaseEntity<Model> {
    @Column(name = "id", primaryKey = true)
    private UUID id;
    /**
     * 父id
     */
    @Column(name = "parent_id")
    private UUID parentId;
    /**
     * 节点类型
     */
    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String desc;

    @Column(name = "provider")
    private String provider;

    @Column(name = "model_type")
    private String modelType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "credential")
    private String credential;

    @Column(name = "model_params_form")
    private JsonArray modelParamsForm;

    @Column(name = "meta")
    private JsonObject meta;

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


    @Override
    @JsonIgnore
    public Map<DatabaseType, BaseConvert<Model>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite(),
                DatabaseType.POSTGRESQL, new Pgsql(),
                DatabaseType.H2, new Pgsql());
    }


    static class Pgsql implements BaseConvert<Model> {
        @Override
        public Model mapTo(Row row) {
            Model model = new Model();
            model.id = row.getUUID("id");
            model.parentId = row.getUUID("parent_id");
            model.type = row.getString("type");
            model.name = row.getString("name");
            model.desc = row.getString("desc");
            model.provider = row.getString("provider");
            model.modelType = row.getString("model_type");
            model.modelName = row.getString("model_name");
            model.credential = row.getString("credential");
            model.modelParamsForm = row.getJsonArray("model_params_form");
            model.meta = row.getJsonObject("meta");
            model.star = row.getBoolean("star");
            model.share = row.getBoolean("share");
            model.createTime = row.getLocalDateTime("create_time");
            model.updateTime = row.getLocalDateTime("update_time");
            return model;
        }
    }

    class Sqlite implements BaseConvert<Model> {
        @Override
        public Map<String, Object> toMap(Model model) {
            Map<String, Object> map = BaseConvert.super.toMap(model);
            map.put("meta", JacksonUtils.toJson(model.meta));
            map.put("model_params_form", JacksonUtils.toJson(model.modelParamsForm));
            return map;
        }

        @Override
        public Model mapTo(Row row) {
            Model model = new Model();
            model.id = row.getUUID("id");
            model.parentId = row.getUUID("parent_id");
            model.type = row.getString("type");
            model.name = row.getString("name");
            model.desc = row.getString("desc");
            model.provider = row.getString("provider");
            model.modelType = row.getString("model_type");
            model.modelName = row.getString("model_name");
            model.credential = row.getString("credential");
            model.modelParamsForm = JacksonUtils.fromJson(row.getString("model_params_form"), JsonArray.class);
            model.meta = JacksonUtils.fromJson(row.getString("meta"), JsonObject.class);
            model.star = row.getInteger("star") != 0;
            model.share = row.getInteger("share") != 0;
            model.createTime = row.getLocalDateTime("create_time");
            model.updateTime = row.getLocalDateTime("update_time");
            return model;
        }
    }

}
