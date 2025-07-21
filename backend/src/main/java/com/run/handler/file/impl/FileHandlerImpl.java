package com.run.handler.file.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.run.common.constants.DatabaseType;
import com.run.common.result.Result;
import com.run.common.util.CommonUtils;
import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import com.run.handler.file.IFileHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

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
    @Inject
    private FileMapper fileMapper;
    @Inject
    @Named("runify.server.datasource.db_type")
    private DatabaseType dbType;
    @Inject
    private Vertx vertx;


    private void uploadSqlite() {

    }

    private void uploadPgsql() {

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
            if (dbType == DatabaseType.POSTGRESQL) {
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
        String node_id = context.pathParam("file_id");
        fileMapper.getById(node_id).compose(file -> {
            String contentType = MimeMapping.mimeTypeForFilename(file.getFileName());
            if (contentType != null) {
                if (contentType.startsWith("text")) {
                    context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=utf-8");
                } else {
                    context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                }
            }
            context.response().putHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            context.response().putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(file.getSize()));
            context.response().putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + file.getFileName());
            context.response().putHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-" + file.getSize() + "/" + file.getSize());
            BaseReadStream baseReadStream = fileMapper.downloadFile(vertx, file);
            baseReadStream.handler(buffer -> {
                if (context.response().closed()) {
                    baseReadStream.close();
                }
                context.response().write(buffer);
            }).endHandler(v -> {
                context.end();
            }).exceptionHandler(context::fail);
            return baseReadStream.read();
        }).onFailure(context::fail);
    }
}
