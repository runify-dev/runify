package com.run.dao.entity;

import com.run.common.constants.ConversationUserType;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.entity.BaseEntity;
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
@Table(schemaName = "public", name = "conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conversation implements BaseEntity<Conversation> {
    @Column(name = "id", primaryKey = true)
    private UUID id;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "name")
    private String name;

    @Column(name = "meta")
    private JsonObject meta;

    @Column(name = "conversation_user_id")
    private UUID conversationUserId;

    @Column(name = "conversation_user_type")
    private ConversationUserType conversationUserType;

    @Column(name = "star_num")
    private Integer starNum;

    @Column(name = "trample_num")
    private Integer trampleNum;

    @Column(name = "mark_sum")
    private Integer markSum;

    @Column(name = "conversation_record_count")
    private Integer conversationRecordCount;


    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
