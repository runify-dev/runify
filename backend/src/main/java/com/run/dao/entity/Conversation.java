package com.run.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.run.common.constants.ConversationUserType;
import com.run.common.constants.DatabaseType;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
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

    class Sqlite implements BaseConvert<Conversation> {
        @Override
        public Conversation mapTo(Row row) {
            Conversation conversation = new Conversation();
            conversation.id = row.getUUID("id");
            conversation.applicationId = row.getUUID("application_id");
            conversation.name = row.getString("name");
            conversation.meta = JacksonUtils.fromJson(row.getString("meta"), JsonObject.class);
            conversation.name = row.getString("name");
            conversation.conversationUserId = row.getUUID("conversation_user_id");
            conversation.conversationUserType = ConversationUserType.valueOf(row.getString("conversation_user_type"));
            conversation.starNum = row.getInteger("star_num");
            conversation.trampleNum = row.getInteger("trample_num");
            conversation.markSum = row.getInteger("mark_sum");
            conversation.conversationRecordCount = row.getInteger("conversation_record_count");
            conversation.isDeleted = row.getInteger("is_deleted") != 0;
            conversation.createTime = row.getLocalDateTime("create_time");
            conversation.updateTime = row.getLocalDateTime("update_time");
            return conversation;
        }
    }

    @Override
    @JsonIgnore
    public Map<DatabaseType, BaseConvert<Conversation>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite());
    }
}
