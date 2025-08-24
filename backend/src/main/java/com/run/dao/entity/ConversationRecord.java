package com.run.dao.entity;

import com.run.common.constants.ConversationUserType;
import com.run.common.constants.DatabaseType;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.annotations.Column;
import com.run.dao.common.annotations.Table;
import com.run.dao.common.convert.BaseConvert;
import com.run.dao.common.entity.BaseEntity;
import io.vertx.core.json.JsonArray;
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

    class Sqlite implements BaseConvert<ConversationRecord> {
        @Override
        public ConversationRecord mapTo(Row row) {
            ConversationRecord conversationRecord = new ConversationRecord();
            conversationRecord.id = row.getUUID("id");
            conversationRecord.applicationId = row.getUUID("application_id");
            conversationRecord.conversationId = row.getUUID("conversation_id");
            conversationRecord.star = row.getInteger("star") != 0;
            conversationRecord.trample = row.getInteger("trample") != 0;
            conversationRecord.question = JacksonUtils.fromJson(row.getString("question"), JsonObject.class);
            conversationRecord.answer = JacksonUtils.fromJson(row.getString("answer"), JsonArray.class);
            conversationRecord.details = JacksonUtils.fromJson(row.getString("details"), JsonObject.class);
            conversationRecord.runTime = row.getFloat("run_time");
            conversationRecord.createTime = row.getLocalDateTime("create_time");
            conversationRecord.updateTime = row.getLocalDateTime("update_time");
            return conversationRecord;
        }
    }

    @Override
    public Map<DatabaseType, BaseConvert<ConversationRecord>> getConvertMap() {
        return Map.of(DatabaseType.SQLITE, new Sqlite());
    }
}
