package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
