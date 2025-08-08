package com.run.handler.tree.impl;


import com.run.common.constants.DatabaseType;
import com.run.common.result.Result;
import com.run.common.util.TreeUtil;
import com.run.dao.common.entity.FullNodeRelation;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.*;
import com.run.dao.mapper.ModelMapper;
import com.run.handler.tree.INodeHandler;
import com.run.handler.tree.pojo.CreateSimpleNodePojo;
import com.run.handler.tree.pojo.QueryNodePojo;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import net.sf.jsqlparser.expression.Expression;
import org.apache.commons.lang3.StringUtils;


import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeHandlerImpl implements INodeHandler {

    private Pool pool;

    private DatabaseType dbType;

    @Inject
    public NodeHandlerImpl(Pool pool, DatabaseType dbType) {
        this.pool = pool;
        this.dbType = dbType;
    }

    Map<String, FullNodeRelation<?, ?>> sourceMap = Map.of(
            "knowledge", new FullNodeRelation<>(KnowledgeRelation.getWallNodeRelation(),
                    () -> new BaseMapper<>(pool, dbType, KnowledgeRelation.class),
                    () -> new BaseMapper<>(pool, dbType, Knowledge.class)),
            "application", new FullNodeRelation<>(ApplicationRelation.getWallNodeRelation(),
                    () -> new BaseMapper<>(pool, dbType, ApplicationRelation.class),
                    () -> new BaseMapper<>(pool, dbType, Application.class)),
            "model", new FullNodeRelation<>(ModelRelation.getWallNodeRelation(),
                    () -> new BaseMapper<>(pool, dbType, ModelRelation.class),
                    () -> new ModelMapper(pool, dbType)));

    public Expression getWhere(String resource, QueryNodePojo queryNodePojo) {
        return TreeUtil.getWhere(queryNodePojo, sourceMap.get(resource).getNodeRelationMapper().getTable());

    }

    @Override
    public void list(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, entries.get("type"), entries.get("name"), null, null, null);
        list(context, queryNodePojo, resourceType);
    }

    @Override
    public void listResource(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = TreeUtil.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, resourceType, entries.get("name"), null, null, null);
        list(context, queryNodePojo, resourceType);
    }

    @Override
    public void listTree(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = TreeUtil.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, null, entries.get("name"), null, null, null);
        list(context, queryNodePojo, resourceType);
    }

    public void list(RoutingContext context, QueryNodePojo queryNodePojo, String resource) {
        BaseMapper<?> nodeMapper = sourceMap.get(resource).getNodeMapper();
        Expression where = TreeUtil.getWhere(queryNodePojo, sourceMap.get(resource).getNodeRelationMapper().getTable());
        nodeMapper.list(where, Map.of())
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(e -> context.end(Result.error(e.toString()).toBuffer()));
    }

    @Override
    public void listStar(RoutingContext context) {
        String resource = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resource, entries.get("type"), entries.get("name"), null, true, null);
        list(context, queryNodePojo, resource);
    }

    @Override
    public void listShared(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, entries.get("type"), entries.get("name"), null, null, true);
        list(context, queryNodePojo, resourceType);
    }


    @Override
    public void delete(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        String resource = context.pathParam("resource");
        this.sourceMap.get(resource).delete(resourceId, pool)
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void rename(RoutingContext context) {
        UUID folderId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        String resourceId = context.pathParam("resourceId");
        String resource = context.pathParam("resource");
        String name = context.body().asJsonObject().getString("name");
        this.sourceMap.get(resource)
                .rename(folderId, UUID.fromString(resourceId), name)
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void get(RoutingContext context) {
        UUID folderId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        String resourceId = context.pathParam("resourceId");
        String resource = context.pathParam("resource");
        this.sourceMap.get(resource).getNodeMapper().getById(resourceId)
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void create(RoutingContext context) {
        String resource = context.pathParam("resource");
        UUID folderId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        CreateSimpleNodePojo createSimpleResourcePojo = context.body().asPojo(CreateSimpleNodePojo.class);
        String name = createSimpleResourcePojo.getName();
        FullNodeRelation<?, ?> fullNodeRelation = this.sourceMap.get(resource);
        Future<?> future;
        if (StringUtils.isEmpty(name)) {
            future = fullNodeRelation.create(folderId, createSimpleResourcePojo.getType());
        } else {
            future = fullNodeRelation.create(folderId, createSimpleResourcePojo.getType(), createSimpleResourcePojo.getName());
        }
        future
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);

    }

}
