package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ProjectAiSession;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ProjectAiSessionMapper extends BaseMapper<ProjectAiSession> {
    @Inject
    public ProjectAiSessionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
