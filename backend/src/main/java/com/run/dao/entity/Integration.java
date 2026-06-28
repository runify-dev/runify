package com.run.dao.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
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
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成(企业微信/飞书/微信等) }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "integration")
public class Integration implements BaseEntity<Integration> {
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

    /**
     * 集成类型: WECOM / FEISHU / WECHAT
     */
    @Column(name = "type")
    private String type;

    /**
     * 绑定的应用id(可换绑)
     */
    @Column(name = "application_id")
    private UUID applicationId;

    /**
     * 凭证(RSA 加密后的 JSON 密文)
     */
    @Column(name = "config")
    private String config;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "meta")
    private JsonObject meta;

    /**
     * 创建时间
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

    public String encrypt(JsonObject config) {
        return RSAUtil.encrypt(JacksonUtils.toJson(config.getMap()));
    }

    public JsonObject decrypt() {
        try {
            String decrypt = RSAUtil.decrypt(this.config);
            HashMap<String, Object> config = JacksonUtils.fromJson(decrypt, new TypeReference<HashMap<String, Object>>() {
            });
            return new JsonObject(config);
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
