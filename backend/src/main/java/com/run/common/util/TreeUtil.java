package com.run.common.util;

import com.run.common.exception.ApiException;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.WallNodeRelation;
import com.run.dao.common.mapper.BaseMapper;
import com.run.handler.tree.pojo.QueryNodePojo;
import com.run.sql.DSL;
import com.run.sql.condition.Condition;
import io.vertx.core.Future;
import io.vertx.sqlclient.RowSet;
import org.apache.commons.lang3.StringUtils;


import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/18  00:02}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TreeUtil {


    public static String getParentId(String parentId) {
        if (StringUtils.equals(parentId, "root")) {
            return null;
        }
        return parentId;
    }

    public static UUID getParentUuId(String parentId) {
        if (StringUtils.equals(parentId, "root") || parentId == null) {
            return null;
        }
        return UUID.fromString(parentId);
    }

    /**
     * 获取闭包节点
     *
     * @param parentId 父id
     * @param nodeId   当前节点id
     * @param mapper   mapper
     * @param <T>      闭包节点
     * @return 当前节点所有的闭包节点
     */
    public static <T extends BaseEntity<T>, N extends BaseEntity<N>> Future<List<T>> getNodeRelation(UUID parentId, UUID nodeId,
                                                                                                     BaseMapper<T> mapper,
                                                                                                     WallNodeRelation<T, N> wallNodeRelation
    ) {
        if (parentId == null) {
            return Future.succeededFuture(List.of(
                    wallNodeRelation.apply(UUID.randomUUID(), null, nodeId, 1),
                    wallNodeRelation.apply(UUID.randomUUID(), nodeId, nodeId, 0)));
        }
        return mapper.list(DSL.field("descendant_id").eq(DSL.param("descendant_id")),
                        Map.of("descendant_id", parentId))
                .compose(nodeRelations -> {
                    List<T> result = new ArrayList<>();
                    for (T t : nodeRelations) {
                        T nr = wallNodeRelation.apply(UUID.randomUUID(), wallNodeRelation.getAncestorId(t), nodeId, wallNodeRelation.getDepth(t) + 1);
                        result.add(nr);
                    }
                    result.add(wallNodeRelation.apply(UUID.randomUUID(), nodeId, nodeId, 0));
                    return Future.succeededFuture(result);
                });
    }

    /**
     * 构建查询条件
     *
     * @param queryNodePojo 查询对象
     * @param baseMapper    需要查询的表
     * @return 表达式
     */
    public static Condition getWhere(QueryNodePojo queryNodePojo,
                                     BaseMapper<?> baseMapper
    ) {
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
            condition = condition.and(DSL.field("name").eq(queryNodePojo.getName()));
        }
        if (queryNodePojo.getStar() != null) {
            condition = condition.and(DSL.field("star").eq(queryNodePojo.getStar()));
        }
        if (queryNodePojo.getShare() != null) {
            condition = condition.and(DSL.field("share").eq(queryNodePojo.getShare()));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getType())) {
            if (StringUtils.equals("folder", queryNodePojo.getType())) {
                condition = condition.and(DSL.field("type").eq(queryNodePojo.getType()));
            } else {
                condition = condition.and(DSL.field("type").eq("folder"));
            }
        }
        return DSL.field("id").in(baseMapper.getDslContext().select(DSL.field("descendant_id"))
                .from(baseMapper.getTable())
                .where(condition));


    }

    /**
     * 校验节点名称
     *
     * @param parentId 父节点id
     * @param nodeName 节点名称
     * @param nodeId   节点id
     * @return 是否校验通过
     */
    public static <T extends BaseEntity<T>> Future<Boolean> validateNodeName(UUID parentId, String nodeName, UUID nodeId, BaseMapper<T> mapper) {

        Condition condition = DSL.field("name").eq(DSL.param("name"));
        if (nodeId != null) {
            condition = condition.and(DSL.field("id").notEqual(DSL.param("id")));
        }
        Future<RowSet<T>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = mapper._list(condition.and(DSL.field("parent_id").isNull()), Map.of("name", nodeName, "id", nodeId != null ? nodeId : ""));

        } else {
            rowSetFuture = mapper._list(condition.and(DSL.field("parent_id").eq(DSL.param("parent_id"))),
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
}
