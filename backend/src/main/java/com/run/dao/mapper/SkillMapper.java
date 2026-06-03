package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Skill;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class SkillMapper extends BaseMapper<Skill> {
    @Inject
    public SkillMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
