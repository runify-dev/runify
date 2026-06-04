package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.KnowledgeFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class KnowledgeFolderMapper extends BaseMapper<KnowledgeFolder> {
    @Inject
    public KnowledgeFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
