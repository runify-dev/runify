package com.run.dao.entity;


import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

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
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "desc")
    private String desc;

    @Column(name = "provider")
    private String provider;

    @Column(name = "model_name")
    private String modelType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "credential")
    private Map<String, Object> credential;

    @Column(name = "model_params_form")
    private List<Map<String, Object>> modelParamsForm;

    @Column(name = "meta")
    private Map<String, Object> meta;

    @Override
    public Model mapTo(Row row) {
        return null;
    }
}
