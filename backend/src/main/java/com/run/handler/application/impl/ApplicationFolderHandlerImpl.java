package com.run.handler.application.impl;

import com.run.dao.entity.ApplicationFolder;
import com.run.dao.entity.ApplicationRelation;
import com.run.dao.mapper.ApplicationFolderMapper;
import com.run.dao.mapper.ApplicationRelationMapper;
import com.run.handler.application.IApplicationFolderHandler;
import com.run.handler.common.impl.FolderHandlerImpl;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  23:36}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ApplicationFolderHandlerImpl extends FolderHandlerImpl<ApplicationFolder, ApplicationRelation, ApplicationFolderMapper, ApplicationRelationMapper> implements IApplicationFolderHandler {
    @Inject
    public ApplicationFolderHandlerImpl(ApplicationFolderMapper applicationFolderMapper, ApplicationRelationMapper applicationRelationMapper) {
        super(applicationFolderMapper, applicationRelationMapper);
    }

    @Override
    protected UUID getParentId(ApplicationFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(ApplicationFolder resource, String name) {
        resource.setName(name);
    }


    @Override
    protected String getName(ApplicationFolder resource) {
        return resource.getName();
    }

    @Override
    protected ApplicationRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ApplicationRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected ApplicationFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new ApplicationFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(ApplicationRelation applicationRelation) {
        return applicationRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ApplicationRelation applicationRelation) {
        return applicationRelation.getDepth();
    }
}
