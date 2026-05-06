package com.run.handler.application.vo;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/30  16:17}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ConversationVO {
    private JsonObject content;
    private String workflowRunId;
}
