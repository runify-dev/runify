package com.run.handler.application.pojo;

import com.run.common.constants.ConversationExecuteConstants;
import io.vertx.core.MultiMap;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/25  22:33}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Getter
@Setter
public class ConversationQuery {
    @NotNull(message = "应用id不能为空")
    private String applicationId;

    private String startTime;

    private String endTime;

    private String name;

    private String executeType;

    public ConversationQuery(MultiMap multiMap) {
        this.applicationId = multiMap.get("applicationId");
        this.startTime = multiMap.get("startTime");
        this.endTime = multiMap.get("endTime");
        this.name = multiMap.get("name");
        this.executeType = multiMap.get("executeType");
    }

}
