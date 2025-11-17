package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ModelFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/8  19:53}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelFolderMapper extends BaseMapper<ModelFolder> {
    @Inject
    public ModelFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
