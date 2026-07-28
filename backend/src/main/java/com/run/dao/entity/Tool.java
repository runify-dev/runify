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
@Table(schemaName = "public", name = "tool")
public class Tool implements BaseEntity<Tool> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * 唯一标识，作为 LLM function name，需为合法标识符
     */
    @Column(name = "name")
    private String name;

    /**
     * 展示名
     */
    @Column(name = "label")
    private String label;

    /**
     * 描述（给 LLM 的 function.description）
     */
    @Column(name = "description")
    private String description;

    @Column(name = "icon")
    private String icon;

    /**
     * 运行时: WORKFLOW / JS / PY
     */
    @Column(name = "runtime")
    private String runtime;

    /**
     * 调用参数 schema（复用 dynamics-form-plus 的 FormField[]）
     */
    @Column(name = "input_schema")
    private JsonArray inputSchema;

    /**
     * 返回字段 schema（OutputField[]）
     */
    @Column(name = "output_schema")
    private JsonArray outputSchema;

    /**
     * 配置/凭据字段声明（FormField[] + secret 标记）
     */
    @Column(name = "config_schema")
    private JsonArray configSchema;

    /**
     * 配置值（作者默认值，secret 键加密存储）
     */
    @Column(name = "config")
    private String config;

    /**
     * 运行时实现体：WORKFLOW=图JSON；JS=代码等
     */
    @Column(name = "body")
    private JsonObject body;

    @Column(name = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 加密配置值
     */
    public String encryptConfig(JsonObject value) {
        return RSAUtil.encrypt(JacksonUtils.toJson(value.getMap()));
    }

    /**
     * 解密配置值，失败返回空对象
     */
    public JsonObject decryptConfig() {
        try {
            String decrypted = RSAUtil.decrypt(this.config);
            HashMap<String, Object> map = JacksonUtils.fromJson(decrypted, new TypeReference<HashMap<String, Object>>() {
            });
            return new JsonObject(map);
        } catch (Exception e) {
            return new JsonObject();
        }
    }
}
