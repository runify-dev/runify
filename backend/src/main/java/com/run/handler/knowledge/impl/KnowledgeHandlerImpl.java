package com.run.handler.knowledge.impl;


import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Knowledge;
import com.run.dao.entity.KnowledgeFolder;
import com.run.dao.entity.KnowledgePermission;
import com.run.dao.entity.KnowledgeRelation;
import com.run.dao.mapper.KnowledgeFolderMapper;
import com.run.dao.mapper.KnowledgeMapper;
import com.run.dao.mapper.KnowledgePermissionMapper;
import com.run.dao.mapper.KnowledgeRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.knowledge.IKnowledgeHandler;
import com.run.handler.knowledge.pojo.EditKnowledge;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeHandlerImpl extends ResourceHandlerImpl<Knowledge, KnowledgeFolder, KnowledgePermission, KnowledgeRelation, KnowledgeMapper, KnowledgeFolderMapper, KnowledgePermissionMapper, KnowledgeRelationMapper> implements IKnowledgeHandler {

    @Inject
    public KnowledgeHandlerImpl(KnowledgeMapper knowledgeMapper,
                                KnowledgeFolderMapper knowledgeFolderMapper,
                                KnowledgeRelationMapper knowledgeRelationMapper,
                                KnowledgePermissionMapper knowledgePermissionMapper
    ) {
        super(knowledgeMapper, knowledgeFolderMapper, knowledgeRelationMapper, knowledgePermissionMapper);


    }

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Knowledge knowledge) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(knowledge, simpleNodePojo);
        simpleNodePojo.setType("knowledge");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(KnowledgeFolder knowledgeFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(knowledgeFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    protected KnowledgeRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new KnowledgeRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Knowledge resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Knowledge resource, String name) {
        resource.setName(name);
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
    protected String getName(Knowledge resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(KnowledgePermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(KnowledgePermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建知识库";
    }

    @Override
    protected Knowledge newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Knowledge(resourceId, parentUuId, name, "", "", "", false, false, new JsonObject(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected KnowledgePermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new KnowledgePermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
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
        resourceMapper.update(knowledge).
                onSuccess(ok -> {
                    context.end(Result.success(knowledge).toBuffer());
                }).onFailure(context::fail);
    }
}
