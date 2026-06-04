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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(schemaName = "public", name = "skill")
public class Skill implements BaseEntity<Skill> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name")
    private String name;

    @Column(name = "icon")
    private String icon;

    @Column(name = "desc")
    private String desc;

    @Column(name = "parameter_value")
    private String parameterValue;

    @Column(name = "skill_parameter_form")
    private JsonArray skillParameterForm;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public String encrypt(JsonObject value) {
        return RSAUtil.encrypt(JacksonUtils.toJson(value.getMap()));
    }

    public JsonObject decrypt() {
        try {
            String decrypted = RSAUtil.decrypt(this.parameterValue);
            HashMap<String, Object> map = JacksonUtils.fromJson(decrypted, new TypeReference<HashMap<String, Object>>() {});
            return new JsonObject(map);
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
