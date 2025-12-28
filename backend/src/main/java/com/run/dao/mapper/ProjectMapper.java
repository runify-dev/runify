package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Project;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  18:59}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectMapper extends BaseMapper<Project> {
    @Inject
    public ProjectMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
