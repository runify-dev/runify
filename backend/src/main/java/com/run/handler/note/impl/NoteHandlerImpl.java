package com.run.handler.note.impl;


import com.run.auth.constants.PermissionConstants;
import com.run.auth.dto.UserProfile;
import com.run.common.cache.CacheStore;
import com.run.common.result.Result;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchDocument;
import com.run.common.search.SearchQuery;
import com.run.common.util.CommonUtils;
import com.run.common.util.MarkdownChunker;
import com.run.dao.entity.Note;
import com.run.dao.entity.NoteFolder;
import com.run.dao.entity.NotePermission;
import com.run.dao.entity.NoteRelation;
import com.run.dao.mapper.NoteFolderMapper;
import com.run.dao.mapper.NoteMapper;
import com.run.dao.mapper.NotePermissionMapper;
import com.run.dao.mapper.NoteRelationMapper;
import com.run.handler.common.impl.ResourceHandlerImpl;
import com.run.handler.common.pojo.SimpleNodePojo;
import com.run.handler.note.INoteHandler;
import com.run.handler.note.pojo.EditNote;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/14  22:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class NoteHandlerImpl extends ResourceHandlerImpl<Note, NoteFolder, NotePermission, NoteRelation, NoteMapper, NoteFolderMapper, NotePermissionMapper, NoteRelationMapper> implements INoteHandler {
    private final SearchClient searchClient;

    @Inject
    public NoteHandlerImpl(NoteMapper noteMapper,
                           NoteFolderMapper noteFolderMapper,
                           NoteRelationMapper noteRelationMapper,
                           NotePermissionMapper notePermissionMapper,
                           CacheStore cacheStore,
                           SearchClient searchClient
    ) {
        super(noteMapper, noteFolderMapper, noteRelationMapper, notePermissionMapper, cacheStore);
        this.searchClient = searchClient;

    }

    @Override
    protected SimpleNodePojo resourceToSimpleNodePojo(Note note) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(note, simpleNodePojo);
        simpleNodePojo.setType("note");
        return simpleNodePojo;
    }

    @Override
    protected SimpleNodePojo folderToSimpleNodePojo(NoteFolder noteFolder) {
        SimpleNodePojo simpleNodePojo = new SimpleNodePojo();
        CommonUtils.copyProperties(noteFolder, simpleNodePojo);
        simpleNodePojo.setType("folder");
        return simpleNodePojo;
    }

    @Override
    public Boolean resourceRead(RoutingContext context) {
        UserProfile userProfile = context.user().get("user");
        PermissionConstants.Permission permission = PermissionConstants.NOTE_READ.getPermission();
        return userProfile.getPermissions().containsKey(permission.toString());
    }

    @Override
    protected NoteRelation newRelation(UUID id, UUID ancestorId, UUID descendantId, Integer dept) {
        return new NoteRelation(id, ancestorId, descendantId, dept);
    }

    @Override
    protected UUID getParentId(Note resource) {
        return resource.getParentId();
    }

    @Override
    protected void setName(Note resource, String name) {
        resource.setName(name);
    }

    @Override
    protected UUID getAncestorId(NoteRelation noteRelation) {
        return noteRelation.getAncestorId();
    }

    @Override
    protected Integer getDepth(NoteRelation noteRelation) {
        return noteRelation.getDepth();
    }

    @Override
    protected String getName(Note resource) {
        return resource.getName();
    }

    @Override
    protected UUID getTarget(NotePermission permission) {
        return permission.getTarget();
    }

    @Override
    protected String getPermission(NotePermission permission) {
        return permission.getPermission();
    }

    @Override
    protected String getNamePrefix() {
        return "新建笔记";
    }

    @Override
    protected Note newResource(UUID resourceId, UUID parentUuId, String name, RoutingContext context) {
        return new Note(resourceId, parentUuId, name, "", "", "", false, false, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    protected NotePermission newPermission(UUID id, UUID userId, UUID target, String permission) {
        return new NotePermission(id, userId, target, permission, LocalDateTime.now(), LocalDateTime.now());
    }

    @Override
    public void edit(RoutingContext context) {
        String resourceId = context.pathParam("resourceId");
        EditNote editNote = context.body().asPojo(EditNote.class);
        resourceMapper.getById(resourceId).compose(n -> {
                    n.setContent(editNote.getContent());
                    if (StringUtils.isNotEmpty(editNote.getContent())) {
                        n.setExcerpt(editNote.getContent().substring(0, Math.min(editNote.getContent().length(), 64)));
                    }
                    return resourceMapper.update(n).compose(_ -> Future.succeededFuture(n));
                }).
                onSuccess(n -> {
                    context.end(Result.success(n).toBuffer());
                    searchClient.deleteByQuery(SearchQuery.builder("note").exactFilter("noteId", resourceId).build())
                            .thenAccept(_ -> {
                                SearchDocument searchDocument = new SearchDocument(
                                        "note",
                                        CommonUtils.uuid7().toString(),
                                        Map.of(
                                                "title", n.getName(),
                                                "content", n.getContent(),
                                                "folderId", n.getParentId() == null ? "root" : n.getParentId().toString(),
                                                "noteId", resourceId
                                        )
                                );
                                searchClient.index(searchDocument);
                            });


                }).onFailure(context::fail);
    }
}
