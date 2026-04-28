package com.run.handler.file.impl;


import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import com.run.handler.file.IFileHandler;
import com.run.sql.dialect.SQLDialect;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/30  22:43}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FileHandlerImpl implements IFileHandler {
    private final FileMapper fileMapper;

    private final SQLDialect dbType;

    private final Vertx vertx;

    @Inject
    public FileHandlerImpl(FileMapper fileMapper, SQLDialect dbType, Vertx vertx) {
        this.fileMapper = fileMapper;
        this.dbType = dbType;
        this.vertx = vertx;
    }


    @Override
    public void upload(RoutingContext context) {
        List<FileUpload> uploads = context.fileUploads();
        for (FileUpload upload : uploads) {
            String s = upload.uploadedFileName();
            long size = upload.size();
            File file = new File(s);
            FileEntity fileEntity = new FileEntity();
            fileEntity.setId(UUID.randomUUID());
            fileEntity.setSize(size);
            fileEntity.setFileName(upload.fileName());
            fileEntity.setCreateTime(LocalDateTime.now());
            fileEntity.setUpdateTime(LocalDateTime.now());
            fileEntity.setMeta(new JsonObject());
            fileEntity.setRef(null);
            fileEntity.setRefType(null);
            if (dbType == SQLDialect.POSTGRESQL) {
                fileMapper.save(fileEntity, file, 1024 * 1024).onSuccess(ok -> {
                    context.end(Result.success(fileEntity).toBuffer());
                }).onFailure(context::fail);
            } else {
                vertx.executeBlocking(() -> {
                            String sha256 = CommonUtils.getSHA256(file);
                            fileEntity.setSha256Hash(sha256);
                            return sha256;
                        }, false)
                        .compose(sha256 -> fileMapper.search(
                                field(FileEntity::getSha256Hash).eq(sha256),
                                Map.of()))
                        .compose(rows -> vertx.executeBlocking(() -> {
                            if (rows.size() == 0) {
                                Path ossPath = CommonUtils.getOssPath();
                                copyToOss(fileEntity, s, ossPath);
                            } else {
                                String path = rows.iterator().next().getPath();
                                if (StringUtils.isEmpty(path)) {
                                    Path ossPath = CommonUtils.getOssPath();
                                    copyToOss(fileEntity, s, ossPath);
                                } else {
                                    Path target = Paths.get(path);
                                    if (!Files.exists(target)) {
                                        copyToOss(fileEntity, s, target);
                                    } else {
                                        fileEntity.setPath(path);
                                    }

                                }
                            }
                            return fileEntity;
                        }, false))
                        .compose(fileMapper::save)
                        .onSuccess(ok -> context.end(Result.success(fileEntity).toBuffer()))
                        .onFailure(context::fail);
            }
        }
    }

    private void copyToOss(FileEntity fileEntity, String sourcePath, Path targetPath) throws IOException {
        if (!Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }
        fileEntity.setPath(targetPath.toString());
        Files.copy(Paths.get(sourcePath), targetPath);
    }

    @Override
    public void download(RoutingContext context) {
        String fileId = context.pathParam("fileId");
        fileMapper.getById(fileId).compose(file -> {
            String contentType = MimeMapping.mimeTypeForFilename(file.getFileName());
            if (contentType != null) {
                if (contentType.startsWith("text")) {
                    context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=utf-8");
                } else {
                    context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                }
            }

            long fileSize = file.getSize();
            String rangeHeader = context.request().getHeader(HttpHeaderNames.RANGE);

            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] parts = rangeHeader.substring(6).split("-");
                try {
                    if (!parts[0].isEmpty()) {
                        start = Long.parseLong(parts[0]);
                    }
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                } catch (NumberFormatException e) {
                    context.response().setStatusCode(416).end("Range Not Satisfiable");
                    return Future.succeededFuture();
                }

                if (start > end || end >= fileSize || start < 0) {
                    context.response()
                            .setStatusCode(416)
                            .putHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .end("Range Not Satisfiable");
                    return Future.succeededFuture();
                }
            }

            long contentLength = end - start + 1;
            boolean isRange = rangeHeader != null;

            context.response()
                    .setStatusCode(isRange ? 206 : 200)
                    .putHeader(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength))
                    .putHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getFileName());

            BaseReadStream baseReadStream = fileMapper.downloadFile(vertx, file, start, end + 1);

            AtomicBoolean streamClosed = new AtomicBoolean(false);
            Runnable closeStream = () -> {
                if (streamClosed.compareAndSet(false, true)) {
                    baseReadStream.close();
                }
            };

            Runnable clearHandlers = () -> {
                context.response().closeHandler(null);
                context.response().exceptionHandler(null);
            };

            context.response().closeHandler(v -> {
                clearHandlers.run();
                closeStream.run();
            });

            context.response().exceptionHandler(e -> {
                clearHandlers.run();
                closeStream.run();
                context.fail(e);
            });

            baseReadStream.handler(buffer -> {
                if (context.response().closed()) {
                    closeStream.run();
                    return;
                }
                context.response().write(buffer);
                if (context.response().writeQueueFull()) {
                    baseReadStream.pause();
                    context.response().drainHandler(v -> {
                        context.response().drainHandler(null);
                        baseReadStream.resume();
                    });
                }
            }).endHandler(v -> {
                clearHandlers.run();
                closeStream.run();
                context.end();
            }).exceptionHandler(e -> {
                clearHandlers.run();
                closeStream.run();
                context.fail(e);
            });

            return baseReadStream.read();
        }).onFailure(context::fail);
    }
}
