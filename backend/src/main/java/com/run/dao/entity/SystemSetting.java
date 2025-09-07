package com.run.dao.entity;

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

import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/3  22:54}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Table(schemaName = "public", name = "system_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting implements BaseEntity<SystemSetting> {

    /**
     * 节点类型
     */
    @Column(name = "type", primaryKey = true)
    private String type;
    @Column(name = "meta")
    private JsonObject meta;

    @Override
    @JsonIgnore
    public Map<SQLDialect, BaseConvert<SystemSetting>> getConvertMap() {
        return Map.of(SQLDialect.SQLITE, new Sqlite(),
                SQLDialect.POSTGRES, new Pgsql(),
                SQLDialect.H2, new Pgsql());
    }


    static class Pgsql implements BaseConvert<SystemSetting> {
        @Override
        public SystemSetting mapTo(Row row) {
            SystemSetting systemSetting = new SystemSetting();
            systemSetting.type = row.getString("type");
            systemSetting.meta = row.getJsonObject("meta");
            return systemSetting;
        }
    }

    class Sqlite implements BaseConvert<SystemSetting> {
        @Override
        public Map<String, Object> toMap(SystemSetting systemSetting) {
            Map<String, Object> map = BaseConvert.super.toMap(systemSetting);
            map.put("meta", JacksonUtils.toJson(systemSetting.meta));
            return map;
        }

        @Override
        public SystemSetting mapTo(Row row) {
            SystemSetting systemSetting = new SystemSetting();
            systemSetting.type = row.getString("type");
            systemSetting.meta = JacksonUtils.fromJson(row.getString("meta"), JsonObject.class);
            return systemSetting;
        }
    }
}
