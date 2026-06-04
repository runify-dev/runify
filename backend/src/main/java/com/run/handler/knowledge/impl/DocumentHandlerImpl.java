package com.run.handler.knowledge.impl;

import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchDocument;
import com.run.common.search.SearchQuery;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Document;
import com.run.dao.entity.DocumentFolder;
import com.run.dao.mapper.DocumentFolderMapper;
import com.run.dao.mapper.DocumentMapper;
import com.run.handler.knowledge.IDocumentHandler;
import com.run.handler.knowledge.pojo.DocumentTreeItem;
import com.run.sql.DSL;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DocumentHandlerImpl implements IDocumentHandler {
    private final DocumentMapper documentMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final SearchClient searchClient;

    private static final UUID ROOT_FOLDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Inject
    public DocumentHandlerImpl(DocumentMapper documentMapper, DocumentFolderMapper documentFolderMapper, SearchClient searchClient) {
        this.documentMapper = documentMapper;
        this.documentFolderMapper = documentFolderMapper;
        this.searchClient = searchClient;
    }

    @Override
    public void listByParent(RoutingContext context) {
        String knowledgeId = context.pathParam("resourceId");
        String parentId = context.pathParam("documentId");
        Future<List<DocumentFolder>> foldersFuture = documentFolderMapper.list(
                DSL.field("knowledge_id").eq(DSL.param("knowledgeId"))
                        .and(DSL.field("parent_id").eq(DSL.param("parentId"))),
                Map.of("knowledgeId", knowledgeId, "parentId", parentId)
        );
        Future<List<Document>> docsFuture = documentMapper.list(
                DSL.field("knowledge_id").eq(DSL.param("knowledgeId"))
                        .and(DSL.field("parent_id").eq(DSL.param("parentId"))),
                Map.of("knowledgeId", knowledgeId, "parentId", parentId)
        );
        Future.all(foldersFuture, docsFuture).onSuccess(ar -> {
            List<DocumentFolder> folders = foldersFuture.result();
            List<Document> docs = docsFuture.result();
            List<DocumentTreeItem> result = new ArrayList<>();
            for (DocumentFolder f : folders) {
                result.add(new DocumentTreeItem(f.getId(), f.getParentId(), f.getKnowledgeId(),
                        f.getName(), null, "folder", null, null, f.getCreateTime(), f.getUpdateTime()));
            }
            for (Document d : docs) {
                result.add(new DocumentTreeItem(d.getId(), d.getParentId(), d.getKnowledgeId(),
                        d.getName(), d.getIcon(), "document", d.getContent(), d.getExcerpt(),
                        d.getCreateTime(), d.getUpdateTime()));
            }
            context.end(Result.success(result).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void tree(RoutingContext context) {
        String knowledgeId = context.pathParam("resourceId");
        Future<List<DocumentFolder>> foldersFuture = documentFolderMapper.list(
                DSL.field("knowledge_id").eq(DSL.param("knowledgeId")),
                Map.of("knowledgeId", knowledgeId)
        );
        Future<List<Document>> docsFuture = documentMapper.list(
                DSL.field("knowledge_id").eq(DSL.param("knowledgeId")),
                Map.of("knowledgeId", knowledgeId)
        );
        Future.all(foldersFuture, docsFuture).onSuccess(ar -> {
            List<DocumentFolder> folders = foldersFuture.result();
            List<Document> docs = docsFuture.result();
            List<DocumentTreeItem> result = new ArrayList<>();
            for (DocumentFolder f : folders) {
                result.add(new DocumentTreeItem(f.getId(), f.getParentId(), f.getKnowledgeId(),
                        f.getName(), null, "folder", null, null, f.getCreateTime(), f.getUpdateTime()));
            }
            for (Document d : docs) {
                result.add(new DocumentTreeItem(d.getId(), d.getParentId(), d.getKnowledgeId(),
                        d.getName(), d.getIcon(), "document", d.getContent(), d.getExcerpt(),
                        d.getCreateTime(), d.getUpdateTime()));
            }
            context.end(Result.success(result).toBuffer());
        }).onFailure(context::fail);
    }

    @Override
    public void get(RoutingContext context) {
        String documentId = context.pathParam("documentId");
        documentMapper.getById(documentId)
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Boolean> checkNameUnique(String knowledgeId, String parentId, String name, String excludeId) {
        var docCondition = DSL.field("knowledge_id").eq(DSL.param("knowledgeId"))
                .and(DSL.field("parent_id").eq(DSL.param("parentId")))
                .and(DSL.field("name").eq(DSL.param("name")));
        var folderCondition = DSL.field("knowledge_id").eq(DSL.param("knowledgeId"))
                .and(DSL.field("parent_id").eq(DSL.param("parentId")))
                .and(DSL.field("name").eq(DSL.param("name")));
        Map<String, Object> params = Map.of("knowledgeId", knowledgeId, "parentId", parentId, "name", name);
        if (excludeId != null && !excludeId.isEmpty()) {
            docCondition = docCondition.and(DSL.field("id").ne(DSL.param("excludeId")));
            folderCondition = folderCondition.and(DSL.field("id").ne(DSL.param("excludeId")));
            params = Map.of("knowledgeId", knowledgeId, "parentId", parentId, "name", name, "excludeId", excludeId);
        }
        final var finalDocCondition = docCondition;
        final var finalFolderCondition = folderCondition;
        final var finalParams = params;
        return documentMapper.list(finalDocCondition, finalParams).compose(docs -> {
            if (!docs.isEmpty()) {
                return Future.failedFuture(ApiException.of(400, "同级目录已存在同名文档: " + name));
            }
            return documentFolderMapper.list(finalFolderCondition, finalParams).compose(folders -> {
                if (!folders.isEmpty()) {
                    return Future.failedFuture(ApiException.of(400, "同级目录已存在同名文件夹: " + name));
                }
                return Future.succeededFuture(true);
            });
        });
    }

    @Override
    public void createFolder(RoutingContext context) {
        String knowledgeId = context.pathParam("resourceId");
        String parentId = context.pathParam("documentId");
        String name = context.body().asJsonObject().getString("name", "新建文件夹");
        LocalDateTime now = LocalDateTime.now();
        checkNameUnique(knowledgeId, parentId, name, null)
                .compose(_ -> {
                    DocumentFolder folder = new DocumentFolder(
                            UUID.randomUUID(),
                            UUID.fromString(parentId),
                            UUID.fromString(knowledgeId),
                            name, "", now, now
                    );
                    return documentFolderMapper.save(folder).map(folder);
                })
                .onSuccess(folder -> context.end(Result.success(folder).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void createText(RoutingContext context) {
        String knowledgeId = context.pathParam("resourceId");
        String parentId = context.pathParam("documentId");
        String name = context.body().asJsonObject().getString("name", "新建文档.md");
        LocalDateTime now = LocalDateTime.now();
        checkNameUnique(knowledgeId, parentId, name, null)
                .compose(_ -> {
                    Document document = new Document(
                            UUID.randomUUID(),
                            UUID.fromString(parentId),
                            UUID.fromString(knowledgeId),
                            name, "", "", "", now, now
                    );
                    return documentMapper.save(document).map(document);
                })
                .onSuccess(document -> context.end(Result.success(document).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void updateContent(RoutingContext context) {
        String documentId = context.pathParam("documentId");
        String content = context.body().asJsonObject().getString("content", "");
        documentMapper.update(
                        Map.of(DSL.field("content"), DSL.param("content"),
                                DSL.field("update_time"), DSL.param("update_time")),
                        DSL.field("id").eq(DSL.param("id")),
                        Map.of("content", content, "update_time", LocalDateTime.now(), "id", documentId)
                ).compose(_ -> documentMapper.getById(documentId))
                .compose(doc -> {
                    searchClient.deleteByQuery(SearchQuery.builder("document").exactFilter("documentId", documentId).build())
                            .thenAccept(_ -> {
                                SearchDocument searchDocument = new SearchDocument(
                                        "document",
                                        CommonUtils.uuid7().toString(),
                                        Map.of(
                                                "title", doc.getName(),
                                                "content", content,
                                                "knowledgeId", doc.getKnowledgeId().toString(),
                                                "folderId", doc.getParentId() == null ? ROOT_FOLDER_ID.toString() : doc.getParentId().toString(),
                                                "documentId", documentId
                                        )
                                );
                                searchClient.index(searchDocument);
                            });
                    return Future.succeededFuture(doc);
                })
                .onSuccess(doc -> context.end(Result.success(doc).toBuffer()))
                .onFailure(context::fail);
    }

    @Override
    public void rename(RoutingContext context) {
        String documentId = context.pathParam("documentId");
        String name = context.body().asJsonObject().getString("name");
        documentMapper.getById(documentId)
                .compose(existing -> {
                    if (existing != null) {
                        return renameDocument(documentId, existing, name).map(r -> (Object) r);
                    }
                    return renameFolder(documentId, name).map(r -> (Object) r);
                })
                .onSuccess(rs -> context.end(Result.success(rs).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Document> renameDocument(String documentId, Document existing, String name) {
        return checkNameUnique(
                existing.getKnowledgeId().toString(),
                existing.getParentId() != null ? existing.getParentId().toString() : ROOT_FOLDER_ID.toString(),
                name, documentId)
                .compose(_ -> documentMapper.update(
                        Map.of(DSL.field("name"), DSL.param("name"),
                                DSL.field("update_time"), DSL.param("update_time")),
                        DSL.field("id").eq(DSL.param("id")),
                        Map.of("name", name, "update_time", LocalDateTime.now(), "id", documentId)))
                .compose(_ -> documentMapper.getById(documentId));
    }

    private Future<DocumentFolder> renameFolder(String documentId, String name) {
        return documentFolderMapper.getById(documentId)
                .compose(existingFolder -> checkNameUnique(
                        existingFolder.getKnowledgeId().toString(),
                        existingFolder.getParentId() != null ? existingFolder.getParentId().toString() : ROOT_FOLDER_ID.toString(),
                        name, documentId)
                        .compose(_ -> documentFolderMapper.update(
                                Map.of(DSL.field("name"), DSL.param("name"),
                                        DSL.field("update_time"), DSL.param("update_time")),
                                DSL.field("id").eq(DSL.param("id")),
                                Map.of("name", name, "update_time", LocalDateTime.now(), "id", documentId)))
                        .compose(_ -> documentFolderMapper.getById(documentId)));
    }

    @Override
    public void delete(RoutingContext context) {
        String documentId = context.pathParam("documentId");
        documentMapper.getById(documentId)
                .compose(doc -> {
                    if (doc != null) {
                        return documentMapper.deleteById(documentId).map(true);
                    }
                    return deleteFolderById(documentId);
                })
                .onSuccess(_ -> context.end(Result.success(true).toBuffer()))
                .onFailure(context::fail);
    }

    private Future<Boolean> deleteFolderById(String folderId) {
        return documentFolderMapper.getById(folderId)
                .compose(folder -> deleteFolderRecursively(folderId));
    }

    private Future<Boolean> deleteFolderRecursively(String folderId) {
        Future<List<DocumentFolder>> childFoldersFuture = documentFolderMapper.list(
                DSL.field("parent_id").eq(DSL.param("parentId")),
                Map.of("parentId", folderId)
        );
        Future<List<Document>> childDocsFuture = documentMapper.list(
                DSL.field("parent_id").eq(DSL.param("parentId")),
                Map.of("parentId", folderId)
        );
        return Future.all(childFoldersFuture, childDocsFuture).compose(_ -> {
            List<Future<?>> futures = new ArrayList<>();
            for (DocumentFolder child : childFoldersFuture.result()) {
                futures.add(deleteFolderRecursively(child.getId().toString()));
            }
            for (Document child : childDocsFuture.result()) {
                futures.add(documentMapper.deleteById(child.getId().toString()));
            }
            return Future.all(futures).compose(__ -> documentFolderMapper.deleteById(folderId).map(true));
        });
    }
}
