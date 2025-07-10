package com.run.handler.knowledge.impl;

import com.google.inject.Inject;
import com.run.common.result.Result;
import com.run.common.util.ValidatorUtil;
import com.run.common.validator.Group;
import com.run.dao.entity.MarkdownNode;
import com.run.dao.entity.Node;
import com.run.dao.mapper.MarkdownNodeMapper;
import com.run.dao.mapper.NodeMapper;
import com.run.handler.knowledge.IKnowledgeHandler;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeHandlerImpl implements IKnowledgeHandler {
    @Inject
    protected MarkdownNodeMapper markdownNodeMapper;
    @Inject
    protected NodeMapper nodeMapper;

    @Override
    public void edit(RoutingContext context) {
        String node_id = context.pathParam("node_id");
        MarkdownNode markdownNode = context.body().asPojo(MarkdownNode.class);
        markdownNode.setId(UUID.fromString(node_id));
        ValidatorUtil.validate(markdownNode, Group.Edit.class);
        markdownNodeMapper.update(markdownNode).
                compose(ok -> {
                    Node node = new Node();
                    node.setId(markdownNode.getId());
                    node.setExcerpt(markdownNode.getContent().substring(0, Math.min(markdownNode.getContent().length(), 128)));
                    return nodeMapper.update(node);
                }).
                onSuccess(ok -> {
                    context.end(Result.success(markdownNode).toBuffer());
                }).onFailure(context::fail);
    }

    @Override
    public void get(RoutingContext context) {
        String node_id = context.pathParam("node_id");
        markdownNodeMapper.getById(node_id)
                .compose(markdownNode -> {
                    if (markdownNode == null) {
                        MarkdownNode mdNode = new MarkdownNode();
                        mdNode.setId(UUID.fromString(node_id));
                        mdNode.setContent("");
                        mdNode.setUpdateTime(LocalDateTime.now());
                        mdNode.setCreateTime(LocalDateTime.now());
                        return markdownNodeMapper.save(mdNode)
                                .compose(ok -> Future.succeededFuture(mdNode));
                    }
                    return Future.succeededFuture(markdownNode);
                }).onSuccess(markdownNode -> {
                    context.end(Result.success(markdownNode).toBuffer());
                }).onFailure(context::fail);
    }
}
