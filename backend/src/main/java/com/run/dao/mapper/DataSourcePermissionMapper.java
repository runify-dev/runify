package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.DataSourcePermission;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DataSourcePermissionMapper extends BaseMapper<DataSourcePermission> {
    @Inject
    public DataSourcePermissionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
