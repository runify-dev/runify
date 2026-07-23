package com.run.handler.project.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.exception.ApiException;
import com.run.common.project.ProjectManage;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.common.util.TreeUtil;
import com.run.dao.entity.Project;
import com.run.dao.entity.ProjectFolder;
import com.run.dao.entity.ProjectPermission;
import com.run.dao.entity.ProjectRelation;
import com.run.dao.mapper.ProjectFolderMapper;
import com.run.dao.mapper.ProjectMapper;
import com.run.dao.mapper.ProjectPermissionMapper;
import com.run.dao.mapper.ProjectRelationMapper;
import com.run.handler.common.Tool;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.project.IProjectHandler;
import com.run.handler.project.vo.CreateProjectVO;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  18:47}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class ProjectHandlerImpl extends ResourceHandlerImpl<Project, ProjectFolder, ProjectPermission, ProjectRelation, ProjectMapper, ProjectFolderMapper, ProjectPermissionMapper, ProjectRelationMapper> implements IProjectHandler {
    @Inject
    public ProjectHandlerImpl(ProjectMapper projectMapper, ProjectFolderMapper projectFolderMapper, ProjectRelationMapper projectRelationMapper, ProjectPermissionMapper projectPermissionMapper, CacheStore cacheStore) {
        super(projectMapper, projectFolderMapper, projectRelationMapper, projectPermissionMapper, cacheStore);
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
        return new Project(resourceId, parentUuId, name, "", "", "", false, false, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected ProjectPermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new ProjectPermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    public void create(RoutingContext context) {
        UUID parentUuId = TreeUtil.getParentUuId(context.pathParam("folderId"));
        CreateProjectVO createProjectVO = context.body().asPojo(CreateProjectVO.class);
        String name = createProjectVO.getName();
        UUID nodeId = UUID.randomUUID();
        Project resource = new Project(nodeId, parentUuId, name, createProjectVO.getDesc(), createProjectVO.getIcon(), createProjectVO.getPath(), false, false, null, LocalDateTime.now(), LocalDateTime.now());
        Future<Boolean> validPath = resourceMapper.count(field(Project::getPath).eq(createProjectVO.getPath()), Map.of()).compose(c -> {
            if (c > 0) {
                return Future.failedFuture(new ApiException(500, "项目路径已存在"));
            }
            return Future.succeededFuture(Boolean.TRUE);
        });
        Future<Boolean> booleanFuture = Tool.validName(resourceMapper, parentUuId, createProjectVO.getName());
        Future.all(validPath, booleanFuture)
                .compose(ok -> Tool.getNodeRelation(relationMapper, parentUuId, nodeId, this::newRelation, this::getAncestorId, this::getDepth))
                .compose(relationMapper::batch_save)
                .compose(ok -> resourceMapper.save(resource))
                .compose(ok -> Future.succeededFuture(resource))
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
        ;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.PROJECT_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    public void getErrorResponse(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        resourceMapper.getById(resourceId)
                .compose(project -> {
                    if (project == null) {
                        return Future.<Project>failedFuture(new ApiException(500, "不存在的项目ID"));
                    }
                    return Future.succeededFuture(project);
                })
                .onSuccess(project -> context.end(Result.success(project.getErrorResponse()).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void editErrorResponse(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        JsonObject errorResponse = context.body().asJsonObject();
        resourceMapper.getById(resourceId)
                .compose(project -> {
                    if (project == null) {
                        return Future.failedFuture(new ApiException(500, "不存在的项目ID"));
                    }
                    project.setErrorResponse(errorResponse);
                    project.setUpdateTime(LocalDateTime.now());
                    return resourceMapper.update(project).compose(ok -> {
                        // 同步已部署执行器缓存,配置修改即刻生效
                        ProjectManage.updateProject(project.getId(), project);
                        return Future.succeededFuture(project.getErrorResponse());
                    });
                })
                .onSuccess(ok -> context.end(Result.success(ok).toBuffer()))
                .onFailure(context::fail);
    }
}
