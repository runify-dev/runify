package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ProjectAiTask;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class ProjectAiTaskMapper extends BaseMapper<ProjectAiTask> {
    @Inject
    public ProjectAiTaskMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
