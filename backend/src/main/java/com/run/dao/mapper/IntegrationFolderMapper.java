package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.IntegrationFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class IntegrationFolderMapper extends BaseMapper<IntegrationFolder> {
    @Inject
    public IntegrationFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
