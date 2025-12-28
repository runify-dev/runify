package com.run.handler.project.vo;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/23  22:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class EditProcessorVO {
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String desc;
    /**
     * 元数据
     */
    private JsonObject meta;
    /**
     * 工作流
     */
    private JsonObject workflow;
}
