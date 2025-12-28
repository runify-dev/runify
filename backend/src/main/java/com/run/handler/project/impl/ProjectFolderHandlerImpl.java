package com.run.handler.project.impl;

import com.run.dao.entity.NoteFolder;
import com.run.dao.entity.ProjectFolder;
import com.run.dao.entity.ProjectRelation;
import com.run.dao.mapper.ProjectFolderMapper;
import com.run.dao.mapper.ProjectRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.project.IProjectFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  18:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectFolderHandlerImpl extends FolderHandlerImpl<ProjectFolder, ProjectRelation, ProjectFolderMapper, ProjectRelationMapper> implements IProjectFolderHandler {
    @Inject
    public ProjectFolderHandlerImpl(ProjectFolderMapper projectFolderMapper, ProjectRelationMapper projectRelationMapper) {
        super(projectFolderMapper, projectRelationMapper);
    }

    @Override
    protected UUID getParentId(ProjectFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(ProjectFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(ProjectFolder resource) {
        return "新建项目目录";
    }

    @Override
    protected ProjectRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ProjectRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected ProjectFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new ProjectFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(ProjectRelation projectRelation) {
        return projectRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ProjectRelation projectRelation) {
        return projectRelation.getDepth();
    }
}
