package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.DocumentRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DocumentRelationMapper extends BaseMapper<DocumentRelation> {
    @Inject
    public DocumentRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
