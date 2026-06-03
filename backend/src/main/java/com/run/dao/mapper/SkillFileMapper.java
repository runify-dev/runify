package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SkillFile;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class SkillFileMapper extends BaseMapper<SkillFile> {
    @Inject
    public SkillFileMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
