package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.KnowledgePermission;
import com.run.dao.entity.NotePermission;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/8  19:56}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NotePermissionMapper extends BaseMapper<NotePermission> {
    @Inject
    public NotePermissionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
