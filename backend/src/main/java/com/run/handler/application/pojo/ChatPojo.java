package com.run.handler.application.pojo;

import lombok.*;

import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/14  00:21}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatPojo {
    private String question;

    private UUID conversationId;
}
