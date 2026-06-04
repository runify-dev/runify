package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.DocumentFolder;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;

public class DocumentFolderMapper extends BaseMapper<DocumentFolder> {
    @Inject
    public DocumentFolderMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }
}
