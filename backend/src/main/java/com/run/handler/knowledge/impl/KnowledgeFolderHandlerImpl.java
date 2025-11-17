package com.run.handler.knowledge.impl;

import com.run.dao.entity.ApplicationFolder;
import com.run.dao.entity.ApplicationRelation;
import com.run.dao.entity.KnowledgeFolder;
import com.run.dao.entity.KnowledgeRelation;
import com.run.dao.mapper.ApplicationFolderMapper;
import com.run.dao.mapper.ApplicationRelationMapper;
import com.run.dao.mapper.KnowledgeFolderMapper;
import com.run.dao.mapper.KnowledgeRelationMapper;
import com.run.handler.application.IApplicationFolderHandler;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.knowledge.IKnowledgeFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/12  20:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class KnowledgeFolderHandlerImpl extends FolderHandlerImpl<KnowledgeFolder, KnowledgeRelation, KnowledgeFolderMapper, KnowledgeRelationMapper> implements IKnowledgeFolderHandler {
    @Inject
    public KnowledgeFolderHandlerImpl(KnowledgeFolderMapper knowledgeFolderMapper, KnowledgeRelationMapper knowledgeRelationMapper) {
        super(knowledgeFolderMapper, knowledgeRelationMapper);
    }

    @Override
    protected UUID getParentId(KnowledgeFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(KnowledgeFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(KnowledgeFolder resource) {
        return resource.getName();
    }

    @Override
    protected KnowledgeRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new KnowledgeRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected KnowledgeFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new KnowledgeFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(KnowledgeRelation knowledgeRelation) {
        return knowledgeRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(KnowledgeRelation knowledgeRelation) {
        return knowledgeRelation.getDepth();
    }
}
