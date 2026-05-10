package com.run.handler.datasource.vo;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:33}
 * {@code @Version 1.0}
 * {@code @注释: 创建数据源VO}
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateDataSourceVO {

    private String name;

    private String desc;

    private String provider;

    private String dataSourceType;

    private JsonObject meta;
}
