package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.RolePermissionRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class RolePermissionRelationMapper extends BaseMapper<RolePermissionRelation> {
    @Inject
    public RolePermissionRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
