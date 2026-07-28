package com.run.handler.tool.impl;

import com.run.dao.entity.ToolFolder;
import com.run.dao.entity.ToolRelation;
import com.run.dao.mapper.ToolFolderMapper;
import com.run.dao.mapper.ToolRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.tool.IToolFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

public class ToolFolderHandlerImpl extends FolderHandlerImpl<ToolFolder, ToolRelation, ToolFolderMapper, ToolRelationMapper> implements IToolFolderHandler {
    @Inject
    public ToolFolderHandlerImpl(ToolFolderMapper toolFolderMapper, ToolRelationMapper toolRelationMapper) {
        super(toolFolderMapper, toolRelationMapper);
    }

    @Override
    protected UUID getParentId(ToolFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(ToolFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(ToolFolder resource) {
        return resource.getName();
    }

    @Override
    protected ToolRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ToolRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected ToolFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new ToolFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(ToolRelation toolRelation) {
        return toolRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ToolRelation toolRelation) {
        return toolRelation.getDepth();
    }
}
