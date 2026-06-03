package com.run.handler.skill.impl;

import com.run.dao.entity.SkillFolder;
import com.run.dao.entity.SkillRelation;
import com.run.dao.mapper.SkillFolderMapper;
import com.run.dao.mapper.SkillRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.skill.ISkillFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

public class SkillFolderHandlerImpl extends FolderHandlerImpl<SkillFolder, SkillRelation, SkillFolderMapper, SkillRelationMapper> implements ISkillFolderHandler {
    @Inject
    public SkillFolderHandlerImpl(SkillFolderMapper skillFolderMapper, SkillRelationMapper skillRelationMapper) {
        super(skillFolderMapper, skillRelationMapper);
    }

    @Override
    protected UUID getParentId(SkillFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(SkillFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(SkillFolder resource) {
        return resource.getName();
    }

    @Override
    protected SkillRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new SkillRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected SkillFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new SkillFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(SkillRelation skillRelation) {
        return skillRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(SkillRelation skillRelation) {
        return skillRelation.getDepth();
    }
}
