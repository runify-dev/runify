package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.CtxSummary;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class CtxSummaryMapper extends BaseMapper<CtxSummary> {
    @Inject
    public CtxSummaryMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
