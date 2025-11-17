package com.run.dao.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    
    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String desc;

    @Column(name = "icon")
    private String icon;

    @Column(name = "provider")
    private String provider;

    @Column(name = "model_type")
    private String modelType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "credential")
    private String credential;

    @Column(name = "model_parameter_form")
    private JsonArray modelParameterForm;

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

    public String encrypt(JsonObject credential) {
        return RSAUtil.encrypt(JacksonUtils.toJson(credential.getMap()));
    }

    public JsonObject decrypt() {
        try {
            String decrypt = RSAUtil.decrypt(this.credential);
            HashMap<String, Object> credential = JacksonUtils.fromJson(decrypt, new TypeReference<HashMap<String, Object>>() {
            });
            return new JsonObject(credential);
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
