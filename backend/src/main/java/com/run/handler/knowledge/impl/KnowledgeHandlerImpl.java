package com.run.handler.knowledge.impl;

import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
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
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

public class KnowledgeHandlerImpl extends ResourceHandlerImpl<Knowledge, KnowledgeFolder, KnowledgePermission, KnowledgeRelation, KnowledgeMapper, KnowledgeFolderMapper, KnowledgePermissionMapper, KnowledgeRelationMapper> implements IKnowledgeHandler {

    private final KnowledgeMapper knowledgeMapper;

    @Inject
    public KnowledgeHandlerImpl(KnowledgeMapper knowledgeMapper,
                                KnowledgeFolderMapper knowledgeFolderMapper,
                                KnowledgeRelationMapper knowledgeRelationMapper,
                                KnowledgePermissionMapper knowledgePermissionMapper,
                                CacheStore cacheStore) {
        super(knowledgeMapper, knowledgeFolderMapper, knowledgeRelationMapper, knowledgePermissionMapper, cacheStore);
        this.knowledgeMapper = knowledgeMapper;
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditKnowledge pojo = context.body().asPojo(EditKnowledge.class);
        knowledgeMapper.getById(resourceId).compose(knowledge -> {
            if (StringUtils.isNotEmpty(pojo.getName())) knowledge.setName(pojo.getName());
            if (StringUtils.isNotEmpty(pojo.getIcon())) knowledge.setIcon(pojo.getIcon());
            if (pojo.getDesc() != null) knowledge.setDesc(pojo.getDesc());
            knowledge.setUpdateTime(LocalDateTime.now());
            return knowledgeMapper.update(knowledge);
        }).compose(_ -> knowledgeMapper.getById(resourceId))
                .onSuccess(knowledge -> context.end(Result.success(knowledge).toBuffer()))
                .onFailure(context::fail);
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
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.KNOWLEDGE_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
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
        var pojo = context.body().asPojo(com.run.handler.tree.pojo.CreateSimpleNodePojo.class);
        String icon = pojo.getIcon() != null ? pojo.getIcon() : "";
        String desc = pojo.getDesc() != null ? pojo.getDesc() : "";
        return new Knowledge(resourceId, parentUuId, name, icon, desc, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected KnowledgePermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new KnowledgePermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }
}
