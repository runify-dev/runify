package com.run.handler.application.dto;

import com.run.common.constants.ConversationExecuteConstants;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/29  19:06}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public record ConversationDTO(UUID id,
                              UUID applicationId,
                              String name,
                              ConversationExecuteConstants executeType,
                              LocalDateTime createTime,
                              LocalDateTime updateTime) {
}
