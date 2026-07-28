package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ToolPermission;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ToolPermissionMapper extends BaseMapper<ToolPermission> {
    @Inject
    public ToolPermissionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
