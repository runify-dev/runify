package com.run.handler.file.impl;


import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import com.run.handler.file.IFileHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.jooq.SQLDialect;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
            if (dbType == SQLDialect.POSTGRES) {
                fileMapper.save(fileEntity, file, 1024 * 1024).onSuccess(ok -> {
                    context.end(Result.success(fileEntity).toBuffer());
                }).onFailure(context::fail);
            } else {
                String sha256 = CommonUtils.getSHA256(file);
                fileEntity.setSha256Hash(sha256);
                Path ossPath = CommonUtils.getOssPath();
                if (!Files.exists(ossPath.getParent())) {
                    try {
                        Files.createDirectories(ossPath.getParent());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                fileEntity.setPath(ossPath.toString());
                try {
                    Files.copy(Paths.get(s), ossPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                fileMapper.save(fileEntity).onSuccess(ok -> {
                    context.end(Result.success(fileEntity).toBuffer());
                }).onFailure(context::fail);
            }

        }
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

                // range 越界校验
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
            baseReadStream.handler(buffer -> {
                if (context.response().closed()) {
                    baseReadStream.close();
                    return;
                }
                context.response().write(buffer);
            }).endHandler(v -> {
                context.end();
            }).exceptionHandler(context::fail);

            return baseReadStream.read();
        }).onFailure(context::fail);
    }
}
