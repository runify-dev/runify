package com.run.dao.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.run.common.config.AppConfig;
import com.run.common.util.JacksonUtils;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.Integration;
import com.run.handler.integration.IntegrationCredentialMask;
import com.run.sql.dialect.SQLDialect;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class IntegrationMapper extends BaseMapper<Integration> {

    public IntegrationMapper(Pool client, SQLDialect dbType) {
        super(client, dbType, Integration.class);
    }

    @Inject
    public IntegrationMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
    }

    public Future<Map<String, Object>> getResourceById(String id) {
        return getById(id).compose(integration -> {
            if (integration == null) {
                return Future.succeededFuture(null);
            }
            Map<String, Object> result = JacksonUtils.convert(integration, new TypeReference<HashMap<String, Object>>() {
            });
            Map<String, Object> config = integration.decrypt().getMap();
            result.put("config", IntegrationCredentialMask.mask(config));
            return Future.succeededFuture(result);
        });
    }
}
