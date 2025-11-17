package com.run.dao.entity;

import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
@Table(schemaName = "public", name = "conversation_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRecord implements BaseEntity<ConversationRecord> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "star")
    private Boolean star;

    @Column(name = "trample")
    private Boolean trample;

    @Column(name = "question")
    private JsonObject question;

    @Column(name = "answer")
    private JsonArray answer;

    @Column(name = "details")
    private JsonObject details;

    @Column(name = "run_time")
    private Float runTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
