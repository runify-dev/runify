package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Document;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DocumentMapper extends BaseMapper<Document> {
    @Inject
    public DocumentMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
