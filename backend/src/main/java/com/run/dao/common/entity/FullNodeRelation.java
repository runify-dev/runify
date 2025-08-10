package com.run.dao.common.entity;

import com.run.common.util.TreeUtil;
import com.run.dao.common.mapper.BaseMapper;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import lombok.Getter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/18  22:41}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FullNodeRelation<T extends BaseEntity<T>, N extends BaseEntity<N>, NodeMapper extends BaseMapper<N>> {
    BaseMapper<T> nodeRelationMapper;
    BaseMapper<N> nodeMapper;
    WallNodeRelation<T, N> wallNodeRelation;
    Supplier<? extends BaseMapper<T>> newNodeRelationMapper;

    Supplier<? extends BaseMapper<N>> newNodeMapper;

    @Getter
    BiFunction<NodeMapper, String, ? extends Future<?>> getResource;

    public BaseMapper<T> getNodeRelationMapper() {
        if (this.nodeRelationMapper == null) {
            this.nodeRelationMapper = newNodeRelationMapper.get();
        }
        return this.nodeRelationMapper;
    }

    public BaseMapper<N> getNodeMapper() {
        if (this.nodeMapper == null) {
            this.nodeMapper = newNodeMapper.get();
        }
        return this.nodeMapper;
    }

    public FullNodeRelation(WallNodeRelation<T, N> wallNodeRelation,
                            Supplier<? extends BaseMapper<T>> newNodeRelationMapper,
                            Supplier<NodeMapper> newNodeMapper,
                            BiFunction<NodeMapper, String, Future<?>> getResource) {
        this.wallNodeRelation = wallNodeRelation;
        this.newNodeRelationMapper = newNodeRelationMapper;
        this.newNodeMapper = newNodeMapper;
        this.getResource = getResource;
    }

    public WallNodeRelation<T, N> getWallNodeRelation() {
        return wallNodeRelation;
    }

    /**
     * 校验节点名称
     *
     * @param parentId 父节点id
     * @param nodeName 节点名称
     * @param nodeId   节点id
     * @return 是否校验通过
     */
    public Future<Boolean> validateNodeName(UUID parentId, String nodeName, UUID nodeId) {
        return TreeUtil.validateNodeName(parentId, nodeName, nodeId, this.getNodeMapper());
    }

    public Future<String> getNodeName(UUID parentId, String type) {
        Expression andExpression = new LikeExpression().withLeftExpression(new Column("name"))
                .withRightExpression(new Column("#{name}"));
        String prefix = this.wallNodeRelation.getNamePrefixMap().get(type);
        Future<RowSet<N>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = this.getNodeMapper()._list(new AndExpression().withLeftExpression(andExpression)
                    .withRightExpression(new IsNullExpression()
                            .withLeftExpression(new Column("parent_id"))), Map.of("name", prefix + "%"));

        } else {
            rowSetFuture = this.getNodeMapper()._list(new AndExpression().withLeftExpression(andExpression)
                            .withRightExpression(new EqualsTo()
                                    .withLeftExpression(new Column("parent_id"))
                                    .withRightExpression(new Column("#{parent_id}"))),
                    Map.of("name", prefix + "%", "parent_id", parentId));

        }
        return rowSetFuture
                .compose(rowSet -> {
                    List<String> result = new ArrayList<>();
                    rowSet.forEach(n -> result.add(this.wallNodeRelation.getName(n)));
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
     * 获取节点闭包
     *
     * @param parentId 父节点id
     * @param nodeId   节点id
     * @return 当前节点的闭包数据
     */
    public Future<List<T>> getNodeRelation(UUID parentId, UUID nodeId) {
        return TreeUtil.getNodeRelation(parentId, nodeId, this.getNodeRelationMapper(), this.getWallNodeRelation());
    }

    /**
     * 创建
     *
     * @param pId  父id
     * @param type 类型
     * @return 结果
     */
    public Future<N> create(UUID pId, String type) {
        UUID nodeId = UUID.randomUUID();
        return getNodeRelation(pId, nodeId)
                .compose(nodeRelations -> this.getNodeRelationMapper().batch_save(nodeRelations))
                .compose(ok -> getNodeName(pId, type))
                .compose(name -> Future.succeededFuture(this.getWallNodeRelation().build(nodeId, pId, type, name)))
                .compose(n -> this.getNodeMapper().save(n).compose(ok -> Future.succeededFuture(n)));
    }

    /**
     * 创建节点
     *
     * @param pId  pid
     * @param type 类型
     * @param name 节点名称
     * @return 处理器
     */
    public Future<N> create(UUID pId, String type, String name) {
        UUID nodeId = UUID.randomUUID();
        N n = this.getWallNodeRelation().build(nodeId, pId, type, name);
        return validateNodeName(pId, name, null)
                .compose(ok -> getNodeRelation(pId, nodeId))
                .compose(nodeRelations -> this.getNodeRelationMapper().batch_save(nodeRelations))
                .compose(ok -> this.getNodeMapper().save(n))
                .compose(ok -> Future.succeededFuture(n));
    }

    /**
     * 移动节点
     *
     * @param pId 需要移动到那个父节点
     * @param id  需要移动的节点id
     * @return 移动后的节点数据
     */
    public Future<N> move(UUID pId, UUID id) {
        return nodeMapper.getById(id.toString()).compose(node -> validateNodeName(pId, getWallNodeRelation().getName(node), getWallNodeRelation().getId(node)).compose(ok -> Future.succeededFuture(node))).compose(node -> {
            this.getWallNodeRelation().setParentId(node, pId);
            return nodeRelationMapper.delete(new EqualsTo().withLeftExpression(new Column("descendant_id")).withRightExpression(new Column("#{descendant_id}")), Map.of("descendant_id", id)).compose(ok -> getNodeRelation(pId, this.getWallNodeRelation().getId(node))).compose(nodeRelations -> nodeRelationMapper.batch_save(nodeRelations)).compose(ok -> nodeMapper.update(node).compose(r -> Future.succeededFuture(node)));
        });

    }

    /**
     * 修改节点名称
     *
     * @param id   节点id
     * @param name 节点名称
     * @return 异步函数
     */
    public Future<SqlResult<Void>> _rename(UUID id, String name) {
        Update update = new Update();
        update.setTable(this.getNodeMapper().getTable());
        update.setUpdateSets(List.of(new UpdateSet(new Column("name"), new StringValue(name))));
        update.withWhere(new EqualsTo(new Column("id"), new StringValue(id.toString())));
        return nodeMapper.update(update, Map.of());
    }

    public Future<SqlResult<Void>> rename(UUID folderId, UUID id, String name) {
        return validateNodeName(folderId, name, id)
                .compose(ok -> _rename(id, name));
    }

    public Future<?> detail(String id) {
        return this.getResource.apply((NodeMapper) this.getNodeMapper(), id);
    }

    /**
     * 删除节点
     *
     * @param id   节点id
     * @param pool 连接池
     * @return 结果
     */
    public Future<SqlResult<Void>> delete(String id, Pool pool) {
        return this.getNodeMapper()
                .deleteById(id)
                .compose(ok ->
                        this.getNodeRelationMapper()
                                .delete(new EqualsTo()
                                        .withLeftExpression(new Column("descendant_id"))
                                        .withRightExpression(new Column("#{descendant_id}")), Map.of("descendant_id", id))
                );
    }

}
