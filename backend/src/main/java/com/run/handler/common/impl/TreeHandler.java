package com.run.handler.common.impl;

import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.common.util.TreeUtil;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import com.run.handler.common.ITreeHandler;
import com.run.handler.tree.pojo.CreateSimpleNodePojo;
import com.run.handler.tree.pojo.QueryNodePojo;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/9  21:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public abstract class TreeHandler<Node extends BaseEntity<Node>, Relation extends BaseEntity<Relation>, NodeMapper extends BaseMapper<Node>, RelationMapper extends BaseMapper<Relation>> implements ITreeHandler {

    private NodeMapper nodeMapper;

    private RelationMapper relationMapper;

    public TreeHandler(NodeMapper nodeMapper, RelationMapper relationMapper) {
        this.nodeMapper = nodeMapper;
        this.relationMapper = relationMapper;
    }

    public Condition getWhere(QueryNodePojo queryNodePojo) {
        Condition condition = DSL.noCondition();
        if (StringUtils.isNotEmpty(queryNodePojo.getFolderId())) {
            condition = condition.and(DSL.field("ancestor_id").eq(queryNodePojo.getFolderId()));
        } else {
            condition = condition.and(DSL.field("ancestor_id").isNull());
        }
        if (queryNodePojo.getDepth() != null) {
            condition = condition.and(DSL.field("depth").eq(queryNodePojo.getDepth()));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getName())) {
            condition = condition.and(DSL.field("name").eq("%" + queryNodePojo.getName() + "%"));
        }
        if (queryNodePojo.getStar() != null) {
            condition = condition.and(DSL.field("star").eq(queryNodePojo.getStar() ));
        }
        if (queryNodePojo.getShare() != null) {
            condition = condition.and(DSL.field("share").eq(queryNodePojo.getShare()));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getType())) {
            condition = condition.and(DSL.field("type").eq(queryNodePojo.getType()));
        }
        return DSL.field("id").in(relationMapper.getDslContext().select(DSL.field("descendant_id"))
                .from(relationMapper.getTable())
                .where(condition));
    }

    public void list(RoutingContext context, QueryNodePojo queryNodePojo) {
        Condition where = getWhere(queryNodePojo);
        nodeMapper.search(where, Map.of(
                        "folderId", Optional.ofNullable(queryNodePojo.getFolderId()).orElse(""),
                        "depth", Optional.ofNullable(queryNodePojo.getDepth()).orElse(-1),
                        "name", Optional.ofNullable(queryNodePojo.getName()).orElse(""),
                        "star", Optional.ofNullable(queryNodePojo.getStar()).orElse(false),
                        "share", Optional.ofNullable(queryNodePojo.getShare()).orElse(false),
                        "type", Optional.ofNullable(queryNodePojo.getType()).orElse("")))
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(e -> context.end(Result.error(e.toString()).toBuffer()));

    }

    public void list(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, entries.get("type"), entries.get("name"), null, null, null);
        list(context, queryNodePojo);
    }

    public void listResource(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = TreeUtil.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, resourceType, entries.get("name"), null, null, null);
        list(context, queryNodePojo);
    }


    public void listTree(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = TreeUtil.getParentId(context.pathParam("folderId"));
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, null, entries.get("name"), null, null, null);
        list(context, queryNodePojo);
    }

    public void listStar(RoutingContext context) {
        String resource = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resource, entries.get("type"), entries.get("name"), null, true, null);
        list(context, queryNodePojo);
    }


    public void listShared(RoutingContext context) {
        String resourceType = context.pathParam("resource");
        String folderId = context.pathParam("folderId");
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(folderId, resourceType, entries.get("type"), entries.get("name"), null, null, true);
        list(context, queryNodePojo);
    }

    public void get(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        nodeMapper.getById(resourceId)
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }


    public void rename(RoutingContext context) {
        UUID folderId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        String resourceId = context.pathParam("resourceId");
        String name = context.body().asJsonObject().getString("name");
        validateNodeName(folderId, name, UUID.fromString(resourceId)).compose(ok -> {
                    return nodeMapper.update(Map.of(DSL.field("name"), DSL.param("#{name}")),
                            DSL.field("id").eq(DSL.param("#{id}")),
                            Map.of("name", name,
                                    "id", resourceId));
                })
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);

    }

    public Future<SqlResult<Void>> delete(String resourceId, SqlClient sqlClient) {
        return nodeMapper.deleteById(resourceId, sqlClient)
                .compose(ok -> {
                    return relationMapper
                            .delete(DSL.field("descendant_id")
                                            .eq(DSL.param("#{descendant_id}")).or(DSL.field("ancestor_id").eq(DSL.param("#{ancestor_id}"))),
                                    Map.of("descendant_id", resourceId,
                                            "ancestor_id", resourceId), sqlClient);
                });

    }

    public Future<SqlResult<Void>> delete(String resourceId) {
//        return nodeMapper.getClient()
//                .withTransaction(sqlConnection -> {
//                    return delete(resourceId, sqlConnection);
//                });

        return delete(resourceId, nodeMapper.getClient());

    }

    public void delete(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        delete(resourceId)
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);

    }

    /**
     * 校验节点名称是否重名
     *
     * @param parentId 父节点id
     * @param nodeName 节点名称
     * @param nodeId   节点id
     * @return 是否校验通过
     */
    protected Future<Boolean> validateNodeName(UUID parentId, String nodeName, UUID nodeId) {
        Condition condition = DSL.field("name").eq(DSL.param("#{name}"));
        if (nodeId != null) {
            condition = condition.and(DSL.field("id").ne(DSL.param("#{id}")));
        }
        Future<RowSet<Node>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = nodeMapper.search(condition.and(DSL.field("parent_id").isNull()), Map.of("name", nodeName, "id", nodeId != null ? nodeId : ""));

        } else {
            rowSetFuture = nodeMapper.search(condition.and(DSL.field("parent_id").eq(DSL.param("#{parent_id}"))),
                    Map.of("name", nodeName, "parent_id", parentId, "id", nodeId != null ? nodeId : ""));

        }
        return rowSetFuture
                .compose(nodeRelations -> {
                    if (nodeRelations.size() > 0) {
                        throw new ApiException(500, "目录中有重名文件");
                    }
                    return Future.succeededFuture(true);
                });
    }

    public Future<String> getAvailableNodeName(UUID parentId, String type) {
        Condition lick = DSL.field("name").like(DSL.param("#{name}", String.class));
        String prefix = getNamePrefixMap().get(type);
        Future<RowSet<Node>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = nodeMapper._list(lick.and(DSL.field("parent_id").isNull()), Map.of("name", prefix + "%"));
        } else {
            rowSetFuture = nodeMapper._list(DSL.field("parent_id").eq(DSL.param("#{parent_id}")),
                    Map.of("name", prefix + "%", "parent_id", parentId));
        }
        return rowSetFuture
                .compose(rowSet -> {
                    List<String> result = new ArrayList<>();
                    rowSet.forEach(n -> result.add(getNodeName(n)));
                    int i = 0;
                    while (true) {
                        String name = prefix;
                        if (i > 0) {
                            name = prefix + "(" + i + ")";
                        }
                        if (!result.contains(name)) {
                            return Future.succeededFuture(name);
                        }
                        i++;
                    }
                });
    }


    /**
     * 获取闭包节点
     *
     * @param parentId 父id
     * @param nodeId   当前节点id
     * @return 当前节点所有的闭包节点
     */
    public Future<List<Relation>> getNodeRelation(UUID parentId, UUID nodeId) {
        if (parentId == null) {
            return Future.succeededFuture(List.of(
                    newRelation(UUID.randomUUID(), null, nodeId, 1),
                    newRelation(UUID.randomUUID(), nodeId, nodeId, 0)));
        }
        return relationMapper.list(DSL.field("descendant_id").eq(DSL.param("#{descendant_id}")),
                        Map.of("descendant_id", parentId))
                .compose(nodeRelations -> {
                    List<Relation> result = new ArrayList<>();
                    for (Relation t : nodeRelations) {
                        Relation nr = newRelation(UUID.randomUUID(), getAncestorId(t), nodeId, getDepth(t) + 1);
                        result.add(nr);
                    }
                    result.add(newRelation(UUID.randomUUID(), nodeId, nodeId, 0));
                    return Future.succeededFuture(result);
                });
    }

    /**
     * 创建节点
     *
     * @param context 上下文
     */
    public void create(RoutingContext context) {
        String resource = context.pathParam("resource");
        UUID folderId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        CreateSimpleNodePojo createSimpleResourcePojo = context.body().asPojo(CreateSimpleNodePojo.class);
        String name = createSimpleResourcePojo.getName();
        Future<?> future;
        UUID nodeId = UUID.randomUUID();
        if (StringUtils.isEmpty(name)) {
            future = getNodeRelation(folderId, nodeId)
                    .compose(nodeRelations -> relationMapper.batch_save(nodeRelations))
                    .compose(_ -> getAvailableNodeName(folderId, createSimpleResourcePojo.getType()))
                    .compose(newName -> Future.succeededFuture(newNode(nodeId, folderId, createSimpleResourcePojo.getType(), newName)))
                    .compose(n -> nodeMapper.save(n).compose(ok -> Future.succeededFuture(n)));
        } else {
            Node node = newNode(nodeId, folderId, createSimpleResourcePojo.getType(), name);
            future = validateNodeName(folderId, name, null)
                    .compose(ok -> getNodeRelation(folderId, nodeId))
                    .compose(nodeRelations -> relationMapper.batch_save(nodeRelations))
                    .compose(ok -> nodeMapper.save(node))
                    .compose(ok -> Future.succeededFuture(node));
        }
        future
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);

    }

    /**
     * 移动
     *
     * @param nodeId         需要移动的节点
     * @param targetForderId 需要移动到的目录
     * @return 移动后的Node
     */
    public Future<Node> move(UUID nodeId, UUID targetForderId) {
        return nodeMapper.getById(nodeId.toString()).compose(node -> {
            return validateNodeName(targetForderId, getNodeName(node), getNodeId(node)).compose(_ -> Future.succeededFuture(node));
        }).compose(ok -> {
            return nodeMapper.getClient()
                    .withTransaction(
                            sqlConnection -> {
                                return relationMapper
                                        .delete(DSL.field("descendant_id")
                                                        .eq(DSL.param("#{descendant_id}")),
                                                Map.of("descendant_id", nodeId))
                                        .compose(_ -> {
                                            return getNodeRelation(targetForderId, nodeId);
                                        }).compose(relations -> {
                                            return relationMapper.batch_save(relations, sqlConnection);
                                        }).compose(_ -> {
                                            return nodeMapper.update(
                                                    Map.of(DSL.field("parent_id"), DSL.param("#{parent_id}")),
                                                    DSL.field("id").eq(DSL.param("#{id}")),
                                                    Map.of("parent_id", targetForderId, "id", nodeId),
                                                    sqlConnection);
                                        });
                            }
                    ).compose(_ -> Future.succeededFuture(ok));
        });

    }

    /**
     * 获取节点名称
     *
     * @param node 节点数据
     * @return 节点名称
     */
    protected abstract String getNodeName(Node node);

    /**
     * 获取节点id
     *
     * @param node 节点
     * @return 节点id
     */
    protected abstract UUID getNodeId(Node node);

    /**
     * 构建关联关系对象
     *
     * @param id           关联关系id
     * @param ancestorId   前辈id
     * @param descendantId 后辈id
     * @param dept         深度
     * @return 节点闭包关系对象
     */
    protected abstract Relation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept);

    protected abstract Node newNode(UUID id, UUID parentId, String type, String name);

    /**
     * 获取前辈id
     *
     * @param relation 关联关系
     * @return 前辈id
     */
    protected abstract UUID getAncestorId(Relation relation);

    protected abstract Integer getDepth(Relation relation);

    protected abstract Map<String, String> getNamePrefixMap();

}
