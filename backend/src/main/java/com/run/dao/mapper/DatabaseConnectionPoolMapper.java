package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.DatabaseConnectionPool;
import com.run.dao.entity.Processor;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/22  20:32}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DatabaseConnectionPoolMapper extends BaseMapper<DatabaseConnectionPool> {
    @Inject
    public DatabaseConnectionPoolMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
