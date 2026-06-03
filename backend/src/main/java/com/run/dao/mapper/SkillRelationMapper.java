package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SkillRelation;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class SkillRelationMapper extends BaseMapper<SkillRelation> {
    @Inject
    public SkillRelationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
