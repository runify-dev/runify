package com.run.handler.project.impl;

import com.run.common.util.CommonUtils;
import com.run.dao.entity.Project;
import com.run.dao.entity.ProjectFolder;
import com.run.dao.entity.ProjectPermission;
import com.run.dao.entity.ProjectRelation;
import com.run.dao.mapper.ProjectFolderMapper;
import com.run.dao.mapper.ProjectMapper;
import com.run.dao.mapper.ProjectPermissionMapper;
import com.run.dao.mapper.ProjectRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.project.IProjectHandler;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  18:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectHandlerImpl extends ResourceHandlerImpl<Project, ProjectFolder, ProjectPermission, ProjectRelation, ProjectMapper, ProjectFolderMapper, ProjectPermissionMapper, ProjectRelationMapper> implements IProjectHandler {
    @Inject
    public ProjectHandlerImpl(ProjectMapper projectMapper, ProjectFolderMapper projectFolderMapper, ProjectRelationMapper projectRelationMapper, ProjectPermissionMapper projectPermissionMapper) {
        super(projectMapper, projectFolderMapper, projectRelationMapper, projectPermissionMapper);
    }


    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Project project) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(project, simpleNodePojo);
        simpleNodePojo.setType("project");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(ProjectFolder projectFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(projectFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    protected ProjectRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new ProjectRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Project resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Project resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(ProjectRelation projectRelation) {
        return projectRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(ProjectRelation projectRelation) {
        return projectRelation.getDepth();
    }

    @Override
    protected String getName(Project resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(ProjectPermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(ProjectPermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建项目";
    }

    @Override
    protected Project newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Project(resourceId, parentUuId, name, "", "", false, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected ProjectPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new ProjectPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
