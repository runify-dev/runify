package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ToolFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ToolFolderMapper extends BaseMapper<ToolFolder> {
    @Inject
    public ToolFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
