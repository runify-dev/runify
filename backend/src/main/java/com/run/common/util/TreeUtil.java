package com.run.common.util;

import com.run.common.exception.ApiException;
import com.run.dao.common.entity.BaseEntity;
import com.run.dao.common.entity.WallNodeRelation;
import com.run.dao.common.mapper.BaseMapper;
import com.run.handler.tree.pojo.QueryNodePojo;
import io.vertx.core.Future;
import io.vertx.sqlclient.RowSet;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.SelectUtils;
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
        return mapper.list(new EqualsTo()
                                .withLeftExpression(new Column("descendant_id"))
                                .withRightExpression(new Column("#{descendant_id}")),
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
     * @param table         需要查询的表
     * @return 表达式
     */
    public static Expression getWhere(QueryNodePojo queryNodePojo, Table table) {
        Select select = SelectUtils.buildSelectFromTable(table);
        List<Expression> exceptionList = new ArrayList<>();
        if (StringUtils.isNotEmpty(queryNodePojo.getFolderId())) {
            EqualsTo equalsTo = new EqualsTo().withLeftExpression(new Column("ancestor_id"))
                    .withRightExpression(new StringValue(queryNodePojo.getFolderId()));
            exceptionList.add(equalsTo);
        } else {
            exceptionList.add(new IsNullExpression().withLeftExpression(new Column("ancestor_id")));
        }
        if (queryNodePojo.getDepth() != null) {
            exceptionList.add(new EqualsTo().withLeftExpression(new Column("depth"))
                    .withRightExpression(new LongValue(queryNodePojo.getDepth())));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getName())) {
            exceptionList.add(new LikeExpression().withLeftExpression(new Column("name"))
                    .withRightExpression(new StringValue(queryNodePojo.getName())));
        }
        if (queryNodePojo.getStar() != null) {
            exceptionList.add(new IsBooleanExpression().withLeftExpression(new Column("star"))
                    .withIsTrue(queryNodePojo.getStar()));
        }
        if (queryNodePojo.getShare() != null) {
            exceptionList.add(new IsBooleanExpression().withLeftExpression(new Column("share"))
                    .withIsTrue(queryNodePojo.getShare()));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getType())) {
            if (StringUtils.equals("folder", queryNodePojo.getType())) {
                exceptionList.add(new EqualsTo().withLeftExpression(new Column("type"))
                        .withRightExpression(new StringValue(queryNodePojo.getType())));
            } else {
                exceptionList.add(new NotEqualsTo().withLeftExpression(new Column("type"))
                        .withRightExpression(new StringValue("folder")));
            }

        }
        Optional<Expression> reduce = exceptionList.stream().reduce((pre, next) -> new AndExpression().withLeftExpression(pre).withRightExpression(next));
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(reduce.orElse(null));
        plainSelect.withSelectItems(List.of(SelectItem.from(new Column("descendant_id"))));
        return new InExpression().withLeftExpression(new Column("id"))
                .withRightExpression(new Column("(%s)".formatted(select.toString())));

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
        Expression andExpression = new EqualsTo().withLeftExpression(new Column("name"))
                .withRightExpression(new Column("#{name}"));
        if (nodeId != null) {
            andExpression = new AndExpression().withLeftExpression(andExpression)
                    .withRightExpression(new NotEqualsTo().withLeftExpression(new Column("id"))
                            .withRightExpression(new Column("#{id}")));
        }
        Future<RowSet<T>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = mapper._list(new AndExpression().withLeftExpression(andExpression)
                    .withRightExpression(new IsNullExpression()
                            .withLeftExpression(new Column("parent_id"))), Map.of("name", nodeName, "id", nodeId != null ? nodeId : ""));

        } else {
            rowSetFuture = mapper._list(new AndExpression().withLeftExpression(andExpression)
                            .withRightExpression(new EqualsTo()
                                    .withLeftExpression(new Column("parent_id"))
                                    .withRightExpression(new Column("#{parent_id}"))),
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
