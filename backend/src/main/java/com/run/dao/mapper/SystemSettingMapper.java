package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SystemSetting;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/3  22:59}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class SystemSettingMapper extends BaseMapper<SystemSetting> {
    @Inject
    public SystemSettingMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
