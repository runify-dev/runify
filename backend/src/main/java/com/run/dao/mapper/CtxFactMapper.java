package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.CtxFact;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class CtxFactMapper extends BaseMapper<CtxFact> {
    @Inject
    public CtxFactMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
