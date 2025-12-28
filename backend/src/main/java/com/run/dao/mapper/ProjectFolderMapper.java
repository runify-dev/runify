package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ProjectFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/8  19:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectFolderMapper extends BaseMapper<ProjectFolder> {
    @Inject
    public ProjectFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
