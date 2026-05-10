package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Datasource;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DatasourceMapper extends BaseMapper<Datasource> {
    @Inject
    public DatasourceMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
