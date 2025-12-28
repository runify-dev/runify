package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ProjectRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/20  01:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectRelationMapper extends BaseMapper<ProjectRelation> {
    @Inject
    public ProjectRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
