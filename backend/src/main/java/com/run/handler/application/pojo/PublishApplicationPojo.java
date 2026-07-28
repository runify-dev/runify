package com.run.handler.application.pojo;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 发布应用:当前画布工作流(草稿)+ 发布备注}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PublishApplicationPojo {
    private JsonObject workflow;
    private String remark;
}
