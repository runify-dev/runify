package com.run.handler.integration.impl;

import com.run.dao.entity.IntegrationFolder;
import com.run.dao.entity.IntegrationRelation;
import com.run.dao.mapper.IntegrationFolderMapper;
import com.run.dao.mapper.IntegrationRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.integration.IIntegrationFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class IntegrationFolderHandlerImpl extends FolderHandlerImpl<IntegrationFolder, IntegrationRelation, IntegrationFolderMapper, IntegrationRelationMapper> implements IIntegrationFolderHandler {
    @Inject
    public IntegrationFolderHandlerImpl(IntegrationFolderMapper integrationFolderMapper, IntegrationRelationMapper integrationRelationMapper) {
        super(integrationFolderMapper, integrationRelationMapper);
    }

    @Override
    protected UUID getParentId(IntegrationFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(IntegrationFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(IntegrationFolder resource) {
        return resource.getName();
    }

    @Override
    protected IntegrationRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new IntegrationRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected IntegrationFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new IntegrationFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(IntegrationRelation integrationRelation) {
        return integrationRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(IntegrationRelation integrationRelation) {
        return integrationRelation.getDepth();
    }
}
