package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.util.JacksonUtils;
import com.run.common.util.RSAUtil;
import com.run.datasources.DatasourceProviderConstants;
import com.run.datasources.DataSourceType;
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

@Table(schemaName = "public", name = "datasource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Datasource implements BaseEntity<Datasource> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String desc;

    @Column(name = "data_source_type")
    private DataSourceType dataSourceType;

    @Column(name = "provider")
    private DatasourceProviderConstants provider;

    @Column(name = "meta")
    private String meta;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public String encrypt(JsonObject meta) {
        return RSAUtil.encrypt(JacksonUtils.toJson(meta.getMap()));
    }

    public JsonObject decrypt() {
        try {
            String decrypted = RSAUtil.decrypt(this.meta);
            HashMap<String, Object> map = JacksonUtils.fromJson(decrypted, new TypeReference<HashMap<String, Object>>() {
            });
            return new JsonObject(map);
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
