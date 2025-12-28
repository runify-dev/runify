package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ApplicationRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  21:44}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationRelationMapper extends BaseMapper<ApplicationRelation> {
    @Inject
    public ApplicationRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
