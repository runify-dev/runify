package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.ResourceVersion;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/28}
 * {@code @Version 1.0}
 * {@code @注释: 资源版本(发布历史) Mapper}
 */
public class ResourceVersionMapper extends BaseMapper<ResourceVersion> {
    @Inject
    public ResourceVersionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
