package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ToolRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ToolRelationMapper extends BaseMapper<ToolRelation> {
    @Inject
    public ToolRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
