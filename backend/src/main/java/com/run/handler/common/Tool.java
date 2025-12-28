package com.run.handler.common;

import com.run.common.exception.ApiException;
import com.run.common.function.FourFunction;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.mapper.BaseMapper;
import io.vertx.core.Future;
import io.vertx.sqlclient.RowSet;
import org.apache.commons.lang3.Strings;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  17:37}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class Tool {
    public static <R extends BaseEntity<R>, ResourceMapper extends BaseMapper<R>,
            Relation extends BaseEntity<Relation>, RelationMapper extends BaseMapper<Relation>> Future<Boolean>
    move(ResourceMapper resourceMapper,
         RelationMapper relationMapper,
         Function<R, UUID> getParentId,
         Function<R, String> getName,
         FourFunction<UUID, UUID, UUID, Integer, Relation> newRelation,
         Function<Relation, UUID> getAncestorId,
         Function<Relation, Integer> getDepth,
         String resourceId, String folderId) {
        return resourceMapper.getById(resourceId)
                .compose(resource -> Tool.validateNodeName(resourceMapper, getParentId.apply(resource), getName.apply(resource), UUID.fromString(resourceId)))
                .compose(_ -> relationMapper
                        .delete(DSL.field("descendant_id")
                                        .eq(DSL.param("#{descendant_id}")),
                                Map.of("descendant_id", resourceId))
                ).compose(_ -> Tool.getNodeRelation(relationMapper, UUID.fromString(folderId), UUID.fromString(resourceId), newRelation, getAncestorId, getDepth))
                .compose(relationMapper::batch_save).compose(_ -> resourceMapper.update(
                        Map.of(DSL.field("parent_id"), DSL.param("#{parent_id}")),
                        DSL.field("id").eq(DSL.param("#{id}")),
                        Map.of("parent_id", folderId, "id", resourceId)))
                .compose(_ -> Future.succeededFuture(Boolean.TRUE));
    }

    /**
     * 获取可用的名称
     *
     * @param nodeMapper   mapper
     * @param parentId     父id
     * @param getName      获取节点名称
     * @param getPrefix    获取名称前缀
     * @param <Node>       节点
     * @param <NodeMapper> mapper
     * @return 可用名称
     */
    public static <Node extends BaseEntity<Node>, NodeMapper extends BaseMapper<Node>> Future<String>
    getAvailableNodeName(NodeMapper nodeMapper, UUID parentId, Function<Node, String> getName,
                         Supplier<String> getPrefix) {
        Condition lick = DSL.field("name").like(DSL.param("#{name}", String.class));
        Future<RowSet<Node>> rowSetFuture;
        String prefix = getPrefix.get();
        if (parentId == null) {
            rowSetFuture = nodeMapper._list(lick.and(DSL.field("parent_id").isNull()), Map.of("name", prefix + "%"));
        } else {
            rowSetFuture = nodeMapper._list(DSL.field("parent_id").eq(DSL.param("#{parent_id}")),
                    Map.of("name", prefix + "%", "parent_id", parentId));
        }
        return rowSetFuture
                .compose(rowSet -> {
                    List<String> result = new ArrayList<>();
                    rowSet.forEach(n -> result.add(getName.apply(n)));
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
     * @param relationMapper   闭包Mapper
     * @param parentId         父id
     * @param nodeId           节点id
     * @param newRelation      创建闭包函数
     * @param getAncestorId    获取祖先id
     * @param getDepth         获取闭包深度
     * @param <Relation>       闭包单个对象
     * @param <RelationMapper> 闭包Mapper
     * @return 当前节点所有的闭包节点
     */
    public static <Relation extends BaseEntity<Relation>, RelationMapper extends BaseMapper<Relation>> Future<List<Relation>> getNodeRelation(
            RelationMapper relationMapper,
            UUID parentId,
            UUID nodeId,
            FourFunction<UUID, UUID, UUID, Integer, Relation> newRelation,
            Function<Relation, UUID> getAncestorId,
            Function<Relation, Integer> getDepth) {
        if (parentId == null) {
            return Future.succeededFuture(List.of(
                    newRelation.apply(UUID.randomUUID(), null, nodeId, 1),
                    newRelation.apply(UUID.randomUUID(), nodeId, nodeId, 0)));
        }
        return relationMapper.list(DSL.field("descendant_id").eq(DSL.param("#{descendant_id}")),
                        Map.of("descendant_id", parentId))
                .compose(nodeRelations -> {
                    List<Relation> result = new ArrayList<>();
                    for (Relation t : nodeRelations) {
                        Relation nr = newRelation.apply(UUID.randomUUID(), getAncestorId.apply(t), nodeId, getDepth.apply(t) + 1);
                        result.add(nr);
                    }
                    result.add(newRelation.apply(UUID.randomUUID(), nodeId, nodeId, 0));
                    return Future.succeededFuture(result);
                });
    }

    /**
     * 校验节点名称是否重名
     *
     * @param parentId 父节点id
     * @param nodeName 节点名称
     * @param nodeId   节点id
     * @return 是否校验通过
     */
    public static <Node extends BaseEntity<Node>, Mapper extends BaseMapper<Node>> Future<Boolean> validateNodeName(Mapper mapper, UUID parentId, String nodeName, UUID nodeId) {
        Condition condition = DSL.field("name").eq(DSL.param("#{name}"));
        if (nodeId != null) {
            condition = condition.and(DSL.field("id").notEqual(DSL.param("#{id}")));
        }
        Future<RowSet<Node>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = mapper.search(condition.and(DSL.field("parent_id").isNull()), Map.of("name", nodeName, "id", nodeId != null ? nodeId : ""));

        } else {
            rowSetFuture = mapper.search(condition.and(DSL.field("parent_id").eq(DSL.param("#{parent_id}"))),
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

    public static String getParentId(String parentId) {
        if (Strings.CS.equals(parentId, "root") || parentId == null) {
            return null;
        }
        return parentId;
    }

    public static UUID getParentUuId(String parentId) {
        if (Strings.CS.equals(parentId, "root") || parentId == null) {
            return null;
        }
        return UUID.fromString(parentId);
    }
}
