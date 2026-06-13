package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.SkillFile;
import com.run.sql.DSL;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlResult;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

public class SkillFileMapper extends BaseMapper<SkillFile> {
    @Inject
    public SkillFileMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }

    public Future<SqlResult<Void>> deleteBySkillId(String skillId) {
        return delete(DSL.field("skill_id").eq(skillId), Map.of());
    }
}
