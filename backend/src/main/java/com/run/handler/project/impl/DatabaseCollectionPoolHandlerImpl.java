package com.run.handler.project.impl;

import com.run.common.constants.DatabaseConnectionProtocolConstants;
import com.run.common.project.ProjectManage;
import com.run.common.query.Query;
import com.run.common.result.Result;
import com.run.dao.common.F;
import com.run.dao.entity.DatabaseConnectionPool;
import com.run.dao.mapper.DatabaseConnectionPoolMapper;
import com.run.dao.mapper.ProjectMapper;
import com.run.handler.project.IDatabaseCollectionPoolHandler;
import com.run.handler.project.vo.CreateDatabaseCollectionPoolVO;
import com.run.handler.project.vo.QueryDatabaseCollectionPoolVO;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:31}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class DatabaseCollectionPoolHandlerImpl implements IDatabaseCollectionPoolHandler {
    private final DatabaseConnectionPoolMapper databaseConnectionPoolMapper;
    private final ProjectMapper projectMapper;

    @Inject
    public DatabaseCollectionPoolHandlerImpl(DatabaseConnectionPoolMapper databaseConnectionPoolMapper, ProjectMapper projectMapper) {
        this.databaseConnectionPoolMapper = databaseConnectionPoolMapper;
        this.projectMapper = projectMapper;
    }

    @Override
    public void create(RoutingContext context) {
        String projectId = context.pathParam("projectId");
        CreateDatabaseCollectionPoolVO createDatabaseCollectionPoolVO = context.body().asPojo(CreateDatabaseCollectionPoolVO.class);
        DatabaseConnectionPool databaseConnectionPool = new DatabaseConnectionPool(
                UUID.randomUUID(),
                UUID.fromString(projectId),
                createDatabaseCollectionPoolVO.getName(),
                createDatabaseCollectionPoolVO.getDesc(),
                DatabaseConnectionProtocolConstants.valueOf(createDatabaseCollectionPoolVO.getProtocol()),
                createDatabaseCollectionPoolVO.getMeta(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        databaseConnectionPoolMapper.save(databaseConnectionPool)
                .onSuccess(_ -> {
                    ProjectManage.updatePool(UUID.fromString(projectId), databaseConnectionPool);
                    context.end(Result.success(databaseConnectionPool).toBuffer());
                })
                .onFailure(context::fail);
    }

    private Condition getCondition(QueryDatabaseCollectionPoolVO query) {
        Condition condition = F.field(DatabaseConnectionPool::getProjectId).eq(F.params(DatabaseConnectionPool::getProjectId));
        if (StringUtils.isNotEmpty(query.getName())) {
            condition = condition.and(F.field(DatabaseConnectionPool::getName).like(F.params(DatabaseConnectionPool::getName)));
        }
        if (StringUtils.isNotEmpty(query.getDesc())) {
            condition = condition.and(F.field(DatabaseConnectionPool::getDesc).like(F.params(DatabaseConnectionPool::getDesc)));
        }
        if (StringUtils.isNotEmpty(query.getProtocol())) {
            condition = condition.and(F.field(DatabaseConnectionPool::getProtocol).eq(F.params(DatabaseConnectionPool::getProtocol)));
        }
        return condition;
    }

    @Override
    public void page(RoutingContext context) {
        QueryDatabaseCollectionPoolVO query = Query.format(QueryDatabaseCollectionPoolVO.class, context);

        String projectId = context.pathParam("projectId");
        Condition condition = getCondition(query);
        HashMap<String, Object> params = new HashMap<>() {{
            put("name", "%" + query.getName() + "%");
            put("desc", "%" + query.getDesc() + "%");
            put("protocol", query.getProtocol());
            put("projectId", projectId);
        }};
        if (null != query.getCurrentPage() && null != query.getPageSize()) {
            databaseConnectionPoolMapper
                    .page(condition, query.getCurrentPage(), query.getPageSize(), params)
                    .onSuccess(ok -> {
                        context.end(Result.success(ok).toBuffer());
                    }).onFailure(context::fail);
        } else {
            databaseConnectionPoolMapper
                    .list(condition, params)
                    .onSuccess(ok -> {
                        context.end(Result.success(ok).toBuffer());
                    }).onFailure(context::fail);
        }

    }

    @Override
    public void delete(RoutingContext context) {
        String databaseCollectionPoolId = context.pathParam("databaseCollectionPoolId");
        databaseConnectionPoolMapper.deleteById(databaseCollectionPoolId)
                .onSuccess((ok) -> {
                    context.end(Result.success(true).toBuffer());
                }).onFailure(context::fail);

    }

    @Override
    public void edit(RoutingContext context) {
        String databaseCollectionPoolId = context.pathParam("databaseCollectionPoolId");
        CreateDatabaseCollectionPoolVO createDatabaseCollectionPoolVO = context.body().asPojo(CreateDatabaseCollectionPoolVO.class);
        DatabaseConnectionPool databaseConnectionPool = new DatabaseConnectionPool();
        databaseConnectionPool.setId(UUID.fromString(databaseCollectionPoolId));
        databaseConnectionPool.setName(createDatabaseCollectionPoolVO.getName());
        databaseConnectionPool.setMeta(createDatabaseCollectionPoolVO.getMeta());
        databaseConnectionPool.setDesc(createDatabaseCollectionPoolVO.getDesc());
        databaseConnectionPool.setProtocol(DatabaseConnectionProtocolConstants.valueOf(createDatabaseCollectionPoolVO.getProtocol()));
        databaseConnectionPoolMapper.update(databaseConnectionPool).onSuccess(ok -> {
            context.end(Result.success(databaseConnectionPool).toBuffer());
        }).onFailure(context::fail);
    }
}
