package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.RoleUserRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class RoleUserRelationMapper extends BaseMapper<RoleUserRelation> {
    @Inject
    public RoleUserRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
