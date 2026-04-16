package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Role;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class RoleMapper extends BaseMapper<Role> {
    @Inject
    public RoleMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
