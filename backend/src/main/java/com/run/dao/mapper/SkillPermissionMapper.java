package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SkillPermission;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class SkillPermissionMapper extends BaseMapper<SkillPermission> {
    @Inject
    public SkillPermissionMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
