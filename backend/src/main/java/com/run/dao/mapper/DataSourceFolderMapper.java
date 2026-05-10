package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.DataSourceFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DataSourceFolderMapper extends BaseMapper<DataSourceFolder> {
    @Inject
    public DataSourceFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
