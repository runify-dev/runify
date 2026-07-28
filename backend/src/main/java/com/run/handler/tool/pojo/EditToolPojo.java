package com.run.handler.tool.pojo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Data;

/**
 * 编辑工具入参
 */
@Data
public class EditToolPojo {
    private String name;
    private String label;
    private String description;
    private String icon;
    private String runtime;
    private JsonArray inputSchema;
    private JsonArray outputSchema;
    private JsonArray configSchema;
    /**
     * 配置值（secret 键提交脱敏值时保留原值）
     */
    private JsonObject config;
    /**
     * 运行时实现体（WORKFLOW=图JSON；JS=代码等）
     */
    private JsonObject body;
}
