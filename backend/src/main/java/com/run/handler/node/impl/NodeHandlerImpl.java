package com.run.handler.node.impl;

import com.google.inject.Inject;
import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.common.util.ValidatorUtil;
import com.run.common.validator.Group;
import com.run.dao.entity.Node;
import com.run.dao.entity.NodeRelation;
import com.run.dao.mapper.NodeMapper;
import com.run.dao.mapper.NodeRelationMapper;
import com.run.handler.node.INodeHandler;
import com.run.handler.node.pojo.EditNodePojo;
import com.run.handler.node.pojo.NodePojo;
import com.run.handler.node.pojo.QueryNodePojo;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.SelectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NodeHandlerImpl implements INodeHandler {
    @Inject
    protected Pool pool;
    @Inject
    protected NodeMapper nodeMapper;

    @Inject
    protected NodeRelationMapper nodeRelationMapper;

    public Expression getWhere(QueryNodePojo queryNodePojo) {
        Select select = SelectUtils.buildSelectFromTable(nodeRelationMapper.getTable());
        List<Expression> exceptionList = new ArrayList<>();
        if (StringUtils.isNotEmpty(queryNodePojo.getParentId())) {
            EqualsTo equalsTo = new EqualsTo().withLeftExpression(new Column("ancestor_id"))
                    .withRightExpression(new StringValue(queryNodePojo.getParentId()));
            exceptionList.add(equalsTo);
        } else {
            exceptionList.add(new IsNullExpression().withLeftExpression(new Column("ancestor_id")));
        }
        if (StringUtils.isNotEmpty(queryNodePojo.getSource())) {
            exceptionList.add(new EqualsTo().withLeftExpression(new Column("source"))
                    .withRightExpression(new StringValue(queryNodePojo.getSource())));
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
            exceptionList.add(new LikeExpression().withLeftExpression(new Column("type"))
                    .withRightExpression(new StringValue(queryNodePojo.getType())));
        }
        Optional<Expression> reduce = exceptionList.stream().reduce((pre, next) -> new AndExpression().withLeftExpression(pre).withRightExpression(next));
        PlainSelect plainSelect = select.getPlainSelect();
        plainSelect.withWhere(reduce.orElse(null));
        plainSelect.withSelectItems(List.of(SelectItem.from(new Column("descendant_id"))));
        return new InExpression().withLeftExpression(new Column("id"))
                .withRightExpression(new Column("(%s)".formatted(select.toString())));

    }

    @Override
    public void list(RoutingContext context) {
        MultiMap entries = context.queryParams();
        QueryNodePojo queryNodePojo = new QueryNodePojo(entries);
        Expression where = getWhere(queryNodePojo);
        nodeMapper.list(where, Map.of())
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(e -> context.end(Result.error(e.toString()).toBuffer()));
    }

    /**
     * 校验节点名称
     *
     * @param parentId 父节点id
     * @param source   节点元
     * @param nodeName 节点名称
     * @param nodeId   节点id
     * @return 是否校验通过
     */
    public Future<Boolean> validateNodeName(UUID parentId, String source, String nodeName, UUID nodeId) {
        AndExpression andExpression = new AndExpression().withLeftExpression(
                new EqualsTo().withLeftExpression(new Column("source"))
                        .withRightExpression(new Column("#{source}"))
        ).withRightExpression(new EqualsTo().withLeftExpression(new Column("name"))
                .withRightExpression(new Column("#{name}")));
        if (nodeId != null) {
            andExpression = new AndExpression().withLeftExpression(andExpression)
                    .withRightExpression(new NotEqualsTo().withLeftExpression(new Column("id"))
                            .withRightExpression(new Column("#{id}")));
        }
        Future<RowSet<Node>> rowSetFuture;
        if (parentId == null) {
            rowSetFuture = nodeMapper._list(new AndExpression().withLeftExpression(andExpression)
                    .withRightExpression(new IsNullExpression()
                            .withLeftExpression(new Column("parent_id"))), Map.of("source", source, "name", nodeName));

        } else {
            rowSetFuture = nodeMapper._list(new AndExpression().withLeftExpression(andExpression)
                            .withRightExpression(new EqualsTo()
                                    .withLeftExpression(new Column("parent_id"))
                                    .withRightExpression(new Column("#{parent_id}"))),
                    Map.of("source", source, "name", nodeName, "parent_id", parentId));

        }
        return rowSetFuture
                .compose(nodeRelations -> {
                    if (nodeRelations.size() > 0) {
                        throw new ApiException(500, "目录中有重名文件");
                    }
                    return Future.succeededFuture(true);
                });
    }

    /**
     * 获取节点闭包
     *
     * @param parentId   父节点id
     * @param nodeId     节点id
     * @param nodeType   节点类型
     * @param nodeSource 节点元
     * @return 当前节点的闭包数据
     */
    public Future<List<NodeRelation>> getNodeRelation(UUID parentId, UUID nodeId, String nodeType, String nodeSource, String nodeName) {
        if (parentId == null) {
            return Future.succeededFuture(List.of(
                    new NodeRelation(UUID.randomUUID(), null, nodeId, 1, nodeSource, nodeType, nodeName),
                    new NodeRelation(UUID.randomUUID(), nodeId, nodeId, 0, nodeSource, nodeType, nodeName)));
        }
        return nodeRelationMapper.list(new EqualsTo()
                                .withLeftExpression(new Column("descendant_id"))
                                .withRightExpression(new Column("#{descendant_id}")),
                        Map.of("descendant_id", parentId))
                .compose(nodeRelations -> {
                    List<NodeRelation> result = new ArrayList<>();
                    for (NodeRelation nodeRelation : nodeRelations) {
                        nodeRelation.setDescendantId(nodeId);
                        nodeRelation.setDepth(nodeRelation.getDepth() + 1);
                        nodeRelation.setType(nodeType);
                        result.add(nodeRelation);
                    }
                    result.add(new NodeRelation(UUID.randomUUID(), nodeId, nodeId, 0, nodeSource, nodeType, nodeName));
                    result.add(new NodeRelation(UUID.randomUUID(), null, nodeId, result.size(), nodeSource, nodeType, nodeName));
                    return Future.succeededFuture(result);
                });
    }

    @Override
    public void create(RoutingContext context) {
        NodePojo nodePojo = context.body().asPojo(NodePojo.class);
        // 校验参数
        ValidatorUtil.validate(nodePojo, Group.Create.class);
        Node node = nodePojo.toNode();
        validateNodeName(node.getParentId(), node.getSource(), node.getName(), null)
                .compose(ok -> getNodeRelation(node.getParentId(), node.getId(), node.getType(), node.getSource(), node.getName()))
                .compose(nodeRelations -> nodeRelationMapper.batch_save(nodeRelations))
                .compose(ok -> nodeMapper.save(node))
                .onSuccess(ok -> context.end(Result.success(node).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void edit(RoutingContext context) {
        EditNodePojo editNodePojo = context.body().asPojo(EditNodePojo.class);
        String node_id = context.pathParam("node_id");
        nodeMapper.getById(node_id)
                .compose(node -> {
                    if (StringUtils.isNotEmpty(editNodePojo.getName())) {
                        return validateNodeName(node.getParentId(), node.getSource(), editNodePojo.getName(), node.getId())
                                .compose(ok -> Future.succeededFuture(node));
                    }
                    return Future.succeededFuture(node);
                })
                .compose(next -> {
                    if (StringUtils.isNotEmpty(editNodePojo.getName())) {
                        next.setName(editNodePojo.getName());
                    }
                    if (StringUtils.isNotEmpty(editNodePojo.getParentId())) {
                        next.setParentId(UUID.fromString(editNodePojo.getParentId()));
                        return nodeRelationMapper
                                .delete(new EqualsTo()
                                        .withLeftExpression(new Column("descendant_id"))
                                        .withRightExpression(new Column("#{descendant_id}")), Map.of("descendant_id", node_id))
                                .compose(ok -> getNodeRelation(next.getParentId(), next.getId(), next.getType(), next.getSource(), next.getName()))
                                .compose(nodeRelations -> nodeRelationMapper.batch_save(nodeRelations))
                                .compose(ok -> nodeMapper.update(next).compose(r -> Future.succeededFuture(next)));

                    }
                    return nodeMapper.update(next).compose(r -> Future.succeededFuture(next));
                })
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void delete(RoutingContext context) {
        String node_id = context.pathParam("node_id");
        pool.withTransaction(c -> nodeMapper
                        .deleteById(node_id)
                        .compose(ok ->
                                nodeRelationMapper
                                        .delete(new EqualsTo()
                                                .withLeftExpression(new Column("descendant_id"))
                                                .withRightExpression(new Column("#{descendant_id}")), Map.of("descendant_id", node_id))
                        )
                )
                .onSuccess(ok -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

}
