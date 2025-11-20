package com.run.handler.note.impl;

import com.run.dao.entity.NoteFolder;
import com.run.dao.entity.NoteRelation;
import com.run.dao.mapper.NoteFolderMapper;
import com.run.dao.mapper.NoteRelationMapper;
import com.run.handler.common.impl.FolderHandlerImpl;
import com.run.handler.note.INoteFolderHandler;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/12  20:48}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NoteFolderHandlerImpl extends FolderHandlerImpl<NoteFolder, NoteRelation, NoteFolderMapper, NoteRelationMapper> implements INoteFolderHandler {
    @Inject
    public NoteFolderHandlerImpl(NoteFolderMapper noteFolderMapper, NoteRelationMapper noteRelationMapper) {
        super(noteFolderMapper, noteRelationMapper);
    }

    @Override
    protected UUID getParentId(NoteFolder resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(NoteFolder resource, String name) {
        resource.setName(name);
    }

    @Override
    protected String getName(NoteFolder resource) {
        return resource.getName();
    }

    @Override
    protected NoteRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new NoteRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected NoteFolder newFolder(UUID id, UUID parentId, String name, String desc) {
        LocalDateTime now = LocalDateTime.now();
        return new NoteFolder(id, parentId, name, desc, now, now);
    }

    @Override
    protected UUID getAncestorId(NoteRelation noteRelation) {
        return noteRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(NoteRelation noteRelation) {
        return noteRelation.getDepth();
    }
}
