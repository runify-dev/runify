package com.run.handler.knowledge.impl;

import com.run.common.document.Chunk;
import com.run.common.document.DocumentBoundary;
import com.run.common.document.DocumentProcessor;
import com.run.common.document.DocumentProcessorRegistry;
import com.run.common.document.DocumentSource;
import com.run.common.document.ImageChunk;
import com.run.common.document.TextChunk;
import com.run.common.document.TextSink;
import com.run.common.exception.ApiException;
import com.run.common.result.Result;
import com.run.common.search.SearchClient;
import com.run.common.search.SearchDocument;
import com.run.common.search.SearchQuery;
import com.run.common.util.CommonUtils;
import com.run.dao.entity.Document;
import com.run.dao.entity.DocumentFolder;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.DocumentFolderMapper;
import com.run.dao.mapper.DocumentMapper;
import com.run.dao.mapper.FileMapper;
import com.run.handler.knowledge.IDocumentHandler;
import com.run.handler.knowledge.pojo.DocumentTreeItem;
import com.run.sql.DSL;
import io.vertx.core.Future;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DocumentHandlerImpl implements IDocumentHandler {
    private final DocumentMapper documentMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final SearchClient searchClient;
    private final FileMapper fileMapper;

    private static final UUID ROOT_FOLDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Inject
    public DocumentHandlerImpl(DocumentMapper documentMapper, DocumentFolderMapper documentFolderMapper,
                               SearchClient searchClient, FileMapper fileMapper) {
        this.documentMapper = documentMapper;
        this.documentFolderMapper = documentFolderMapper;
        this.searchClient = searchClient;
        this.fileMapper = fileMapper;
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
                    reindex(doc, content);
                    return Future.succeededFuture(doc);
                })
                .onSuccess(doc -> context.end(Result.success(doc).toBuffer()))
                .onFailure(context::fail);
    }

    /**
     * 删除旧索引并重新索引文档内容(与 {@link #updateContent} 一致的检索行为)。
     */
    private void reindex(Document doc, String content) {
        searchClient.deleteByQuery(SearchQuery.builder("document").exactFilter("documentId", doc.getId().toString()).build())
                .thenAccept(_ -> {
                    SearchDocument searchDocument = new SearchDocument(
                            "document",
                            CommonUtils.uuid7().toString(),
                            Map.of(
                                    "title", doc.getName(),
                                    "content", content,
                                    "knowledgeId", doc.getKnowledgeId().toString(),
                                    "folderId", doc.getParentId() == null ? ROOT_FOLDER_ID.toString() : doc.getParentId().toString(),
                                    "documentId", doc.getId().toString()
                            )
                    );
                    searchClient.index(searchDocument);
                });
    }

    @Override
    public void importDocument(RoutingContext context) {
        String knowledgeId = context.pathParam("resourceId");
        String parentId = context.pathParam("documentId");
        List<FileUpload> uploads = context.fileUploads();
        if (uploads.isEmpty()) {
            context.fail(ApiException.of(400, "未上传文件"));
            return;
        }
        FileUpload upload = uploads.iterator().next();
        String fileName = upload.fileName();
        Path path = Path.of(upload.uploadedFileName());

        // 解析(PDF/DOCX/ZIP 等)+ 建文档/文件夹/索引均可能耗时且需阻塞等待异步入库,整体放到阻塞线程
        context.vertx().<List<Document>>executeBlocking(
                        () -> importBundle(path, fileName, UUID.fromString(knowledgeId), UUID.fromString(parentId)), false)
                .onSuccess(docs -> context.end(Result.success(docs).toBuffer()))
                .onFailure(context::fail);
    }

    /**
     * 用 {@link DocumentProcessor} 提取上传文件并入库为一个或多个文档:普通文件→单文档;
     * zip→按内部条目拆成多文档(用 {@link DocumentBoundary} 分界,还原文件夹树)。未命中处理器的
     * 类型(txt/md/csv)按 UTF-8 纯文本读取为单文档。返回创建出的文档列表。
     */
    private List<Document> importBundle(Path path, String fileName, UUID knowledgeId, UUID rootParent) {
        DocumentSource source = DocumentSource.of(path, fileName);
        DocumentProcessor processor = DocumentProcessorRegistry.getInstance().pick(source);
        BundleImporter importer = new BundleImporter(knowledgeId, rootParent, fileName);
        if (processor == null) {
            try {
                importer.onNext(new TextChunk(fileName, Files.readString(path), false));
            } catch (Exception e) {
                throw new RuntimeException("不支持的文件类型: " + fileName);
            }
            importer.onComplete(Optional.empty());
        } else {
            try (processor) {
                processor.extract(source, importer);
            }
            if (importer.error != null) {
                throw new RuntimeException("文档解析失败: " + importer.error.getMessage(), importer.error);
            }
        }
        if (importer.created.isEmpty()) {
            throw new RuntimeException("未从文件中解析出任何文档");
        }
        return importer.created;
    }

    private static <T> T blockGet(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get();
    }

    /**
     * 有状态的多文档导入 sink:每遇到 {@link DocumentBoundary} 就 flush 上一个文档(按 path 建/复用文件夹树、
     * 建 Document、索引),再开始累积下一个。文本/图片块累积进当前文档;图片入库并原位插入 markdown 引用。
     * 无边界时即视为单文档(初始文档名取上传文件名)。逐文档 best-effort,单个失败不影响其余。
     */
    private final class BundleImporter implements TextSink {
        private final UUID knowledgeId;
        private final UUID rootParent;
        private final Map<String, UUID> folderCache = new HashMap<>();
        private final StringBuilder buf = new StringBuilder();
        private String curTitle;
        private String curPath = "";
        private boolean started = true;
        final List<Document> created = new ArrayList<>();
        Throwable error;

        BundleImporter(UUID knowledgeId, UUID rootParent, String initialTitle) {
            this.knowledgeId = knowledgeId;
            this.rootParent = rootParent;
            this.curTitle = initialTitle;
            this.folderCache.put("", rootParent);
        }

        @Override
        public boolean onNext(Chunk chunk) {
            if (chunk instanceof DocumentBoundary db) {
                flush();
                curTitle = db.getTitle();
                curPath = db.getPath() == null ? "" : db.getPath();
                buf.setLength(0);
                started = true;
            } else if (chunk instanceof TextChunk tc) {
                if (!tc.isPlaceholder() && tc.getContent() != null && !tc.getContent().isBlank()) {
                    appendChunk(buf, tc.isInline(), tc.getContent());
                }
            } else if (chunk instanceof ImageChunk ic) {
                appendChunk(buf, ic.isInline(), persistImage(ic));
            }
            return true;
        }

        @Override
        public void onComplete(Optional<Throwable> err) {
            err.ifPresent(e -> error = e);
            flush();
        }

        /**
         * 把当前累积的文档入库(内容非空才建)。best-effort:失败仅跳过该文档。
         */
        private void flush() {
            if (!started) return;
            started = false;
            String content = buf.toString();
            if (content.isBlank()) return;
            try {
                UUID folderId = ensureFolder(curPath);
                LocalDateTime now = LocalDateTime.now();
                Document doc = new Document(UUID.randomUUID(), folderId, knowledgeId,
                        curTitle, "", content, excerpt(content), now, now);
                blockGet(documentMapper.save(doc));
                reindex(doc, content);
                created.add(doc);
            } catch (Exception ignored) {
            }
        }

        /**
         * 把相对目录路径逐段建成(或复用)文件夹,返回最末层文件夹 id。空路径返回根。
         */
        private UUID ensureFolder(String relPath) throws Exception {
            if (relPath == null || relPath.isEmpty()) return rootParent;
            UUID cached = folderCache.get(relPath);
            if (cached != null) return cached;
            UUID parent = rootParent;
            String acc = "";
            for (String seg : relPath.split("/")) {
                if (seg.isEmpty()) continue;
                acc = acc.isEmpty() ? seg : acc + "/" + seg;
                UUID c = folderCache.get(acc);
                if (c != null) {
                    parent = c;
                    continue;
                }
                UUID fid = findOrCreateFolder(parent, seg);
                folderCache.put(acc, fid);
                parent = fid;
            }
            return parent;
        }

        private UUID findOrCreateFolder(UUID parentId, String name) throws Exception {
            var condition = DSL.field("knowledge_id").eq(DSL.param("knowledgeId"))
                    .and(DSL.field("parent_id").eq(DSL.param("parentId")))
                    .and(DSL.field("name").eq(DSL.param("name")));
            Map<String, Object> params = Map.of("knowledgeId", knowledgeId.toString(),
                    "parentId", parentId.toString(), "name", name);
            List<DocumentFolder> existing = blockGet(documentFolderMapper.list(condition, params));
            if (!existing.isEmpty()) return existing.get(0).getId();
            LocalDateTime now = LocalDateTime.now();
            DocumentFolder folder = new DocumentFolder(UUID.randomUUID(), parentId, knowledgeId, name, "", now, now);
            blockGet(documentFolderMapper.save(folder));
            return folder.getId();
        }
    }

    /**
     * 追加一块内容:inline 为 true 时直接拼接(不加块级分隔),否则以空行分隔成新块。空内容跳过。
     */
    private void appendChunk(StringBuilder sb, boolean inline, String s) {
        if (s == null || s.isEmpty()) return;
        if (sb.isEmpty()) {
            sb.append(s);
        } else if (inline) {
            sb.append(s);
        } else {
            sb.append("\n\n").append(s);
        }
    }

    /**
     * 把提取出的图片入库(fileMapper),返回 markdown 图片引用 {@code ![name](./api/storage/file/{id})};
     * 失败返回 null。当前处于 {@code executeBlocking} 的工作线程,可安全阻塞等待入库 Future。
     * 无论成功失败都删除落盘的临时文件。
     */
    private String persistImage(ImageChunk imageChunk) {
        var upload = imageChunk.getUpload();
        String tmpPath = upload.getUploadedFileName();
        try {
            File file = new File(tmpPath);
            FileEntity entity = fileMapper.upload(upload.getFileName(), upload.getSize(), null, null, file)
                    .toCompletionStage().toCompletableFuture().get();
            String name = upload.getFileName() != null ? upload.getFileName() : "image";
            return "![" + name + "](./api/storage/file/" + entity.getId() + ")";
        } catch (Exception e) {
            return null;
        } finally {
            try {
                Files.deleteIfExists(Path.of(tmpPath));
            } catch (Exception ignored) {
            }
        }
    }

    private String excerpt(String content) {
        if (content == null) return "";
        String plain = content.strip();
        return plain.length() > 200 ? plain.substring(0, 200) : plain;
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
