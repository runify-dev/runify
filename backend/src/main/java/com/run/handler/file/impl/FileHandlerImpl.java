package com.run.handler.file.impl;

import com.google.inject.Inject;
import com.run.common.result.Result;
import com.run.dao.entity.FileEntity;
import com.run.dao.mapper.FileMapper;
import com.run.handler.file.IFileHandler;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
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
            fileMapper.save(fileEntity, file, 1024 * 1024).onSuccess(ok -> {
                context.end(Result.success(fileEntity).toBuffer());
            }).onFailure(context::fail);
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
            context.response().putHeader(HttpHeaders.CONTENT_RANGE,"bytes 0-"+file.getSize()+"/"+file.getSize());
            long chunkSize = 1024 * 64;
            return fileMapper.downloadFile(file.getLoId(), 0L, chunkSize, file.getSize(), r -> {
                if (context.response().closed()) {
                    return Future.failedFuture(new RuntimeException("链接已关闭"));
                }
                return context.response().write(r.getBuffer("data"));
            });
        }).onSuccess(ok -> {
            context.end();
        }).onFailure(context::fail);
    }
}
