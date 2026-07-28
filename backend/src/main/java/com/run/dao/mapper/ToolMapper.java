package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Tool;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ToolMapper extends BaseMapper<Tool> {
    @Inject
    public ToolMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
