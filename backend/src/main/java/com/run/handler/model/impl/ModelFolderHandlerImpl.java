package com.run.handler.model.impl;

import com.run.dao.entity.ModelFolder;
import com.run.dao.entity.ModelRelation;
import com.run.dao.mapper.ModelFolderMapper;
import com.run.dao.mapper.ModelRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.model.IModelFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/12  20:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ModelFolderHandlerImpl extends FolderHandlerImpl<ModelFolder, ModelRelation, ModelFolderMapper, ModelRelationMapper> implements IModelFolderHandler {
    @Inject
    public ModelFolderHandlerImpl(ModelFolderMapper modelFolderMapper, ModelRelationMapper modelRelationMapper) {
        super(modelFolderMapper, modelRelationMapper);
    }

    @Override
    protected UUID getParentId(ModelFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(ModelFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(ModelFolder resource) {
        return resource.getName();
    }

    @Override
    protected ModelRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ModelRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected ModelFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new ModelFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(ModelRelation modelRelation) {
        return modelRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ModelRelation modelRelation) {
        return modelRelation.getDepth();
    }
}
