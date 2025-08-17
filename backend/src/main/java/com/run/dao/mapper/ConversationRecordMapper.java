package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ConversationRecord;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/16  18:12}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ConversationRecordMapper extends BaseMapper<ConversationRecord> {
    @Inject
    public ConversationRecordMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
