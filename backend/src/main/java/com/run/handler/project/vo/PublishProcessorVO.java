package com.run.handler.project.vo;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 发布处理器:当前画布工作流(草稿)+ 发布备注}
 */
@Getter
@Setter
public class PublishProcessorVO {
    private JsonObject workflow;
    private String remark;
}
