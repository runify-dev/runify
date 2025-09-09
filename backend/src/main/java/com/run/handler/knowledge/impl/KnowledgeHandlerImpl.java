package com.run.handler.knowledge.impl;


import com.run.common.result.Result;
import com.run.dao.entity.Knowledge;
import com.run.dao.entity.KnowledgeRelation;
import com.run.dao.mapper.KnowledgeMapper;
import com.run.dao.mapper.KnowledgeRelationMapper;
import com.run.handler.common.impl.TreeHandler;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.pojo.EditKnowledge;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeHandlerImpl extends TreeHandler<Knowledge, KnowledgeRelation, KnowledgeMapper, KnowledgeRelationMapper> implements IKnowledgeHandler {
    private final KnowledgeMapper knowledgeMapper;

    @Inject
    public KnowledgeHandlerImpl(KnowledgeMapper knowledgeMapper, KnowledgeRelationMapper knowledgeRelationMapper) {
        super(knowledgeMapper, knowledgeRelationMapper);
        this.knowledgeMapper = knowledgeMapper;
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditKnowledge editKnowledge = context.body().asPojo(EditKnowledge.class);
        Knowledge knowledge = new Knowledge();
        knowledge.setId(UUID.fromString(resourceId));
        knowledge.setContent(editKnowledge.getContent());
        if (StringUtils.isNotEmpty(editKnowledge.getContent())) {
            knowledge.setExcerpt(editKnowledge.getContent().substring(0, Math.min(editKnowledge.getContent().length(), 64)));
        }
        knowledge.setName(editKnowledge.getName());
        knowledgeMapper.update(knowledge).
                onSuccess(ok -> {
                    context.end(Result.success(knowledge).toBuffer());
                }).onFailure(context::fail);
    }


    @Override
    protected String getNodeName(Knowledge knowledge) {
        return knowledge.getName();
    }

    @Override
    protected UUID getNodeId(Knowledge knowledge) {
        return knowledge.getId();
    }

    @Override
    protected KnowledgeRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new KnowledgeRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected Knowledge newNode(UUID id, UUID parentId, String type, String name) {
        return new Knowledge(id, parentId, name, type, "", "", false, false, new JsonObject(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected UUID getAncestorId(KnowledgeRelation knowledgeRelation) {
        return knowledgeRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(KnowledgeRelation knowledgeRelation) {
        return knowledgeRelation.getDepth();
    }

    @Override
    protected Map<String, String> getNamePrefixMap() {
        return Map.of("md", "新建知识库", "folder", "新建文件夹");
    }
}
