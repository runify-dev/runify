package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Model;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/13  15:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelMapper extends BaseMapper<Model> {
    @Inject
    public ModelMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
