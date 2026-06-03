package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SkillFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class SkillFolderMapper extends BaseMapper<SkillFolder> {
    @Inject
    public SkillFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
