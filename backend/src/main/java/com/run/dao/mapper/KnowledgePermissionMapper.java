package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.KnowledgePermission;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class KnowledgePermissionMapper extends BaseMapper<KnowledgePermission> {
    @Inject
    public KnowledgePermissionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
