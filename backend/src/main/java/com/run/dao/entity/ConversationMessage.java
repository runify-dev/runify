package com.run.dao.entity;

import com.run.common.constants.MessageConstants;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/16  18:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Table(schemaName = "public", name = "conversation_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage implements BaseEntity<ConversationMessage> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "workflow_run_id")
    private UUID workflowRunId;

    @Column(name = "type")
    private MessageConstants type;

    @Column(name = "content")
    private JsonArray content;

    @Column(name = "context")
    private JsonArray context;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
