package com.run.dao.mapper;

import com.run.common.config.AppConfig;
import com.run.common.util.CommonUtils;

import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.FileEntity;
import com.run.sql.dialect.SQLDialect;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.jetbrains.annotations.Nullable;


import javax.inject.Inject;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.run.sql.DSL.field;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/29  20:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FileMapper extends BaseMapper<FileEntity> {
    private Vertx vertx;

    @Inject
    public FileMapper(Pool client, AppConfig appConfig, Vertx vertx) {
        super(client, appConfig);
        this.vertx = vertx;

    }

    /**
     * 获取大对象
     *
     * @param loId   大对象id
     * @param offset 偏移量
     * @param length 查询的大小
     * @return 数据
     */
    public Future<RowSet<Row>> getLargeObject(Long loId, Long offset, Long length) {
        return SqlTemplate.forQuery(client, "SELECT lo_get(" + loId + "," + offset + "," + length + ") as \"data\"").execute(Map.of());
    }

    /**
     * 获取大对象
     *
     * @param loId     大对象id
     * @param size     大小
     * @param function 处理函数
     * @return 数据
     */
    public Future<RowSet<Row>> downloadFile(Long loId, Long size, Function<Row, Future<Void>> function) {
        return downloadFile(loId, 0L, 1024 * 1024L, size, function);
    }

    class PgsqlReadStream implements BaseReadStream {
        private Long loId;
        private Long offset;
        private Long length;
        private Long size;

        private boolean isClose;
        private Handler<Buffer> handler;
        private Handler<Throwable> exceptionHandler;
        private Handler<Void> endHandler;

        public PgsqlReadStream(Long loId, Long offset, Long length, Long size) {
            this.loId = loId;
            this.offset = offset;
            this.length = length;
            this.size = size;
            this.isClose = false;
        }

        @Override
        public Future<Void> read() {
            if (this.handler != null) {
                if (isClose) {
                    return Future.succeededFuture();
                }
                getLargeObject(loId, offset, offset + length > size ? size - offset : length)
                        .onComplete(ok -> {
                            if (isClose) {
                                return;
                            }
                            handler.handle(ok.result().iterator().next().getBuffer("data"));
                            this.offset = offset + length;
                            if (this.offset < size) {
                                read();
                            } else {
                                endHandler.handle(null);
                            }
                        }).onFailure((throwable) -> {
                            exceptionHandler.handle(throwable);
                        });
            }
            return Future.succeededFuture();
        }

        @Override
        public Future<Void> close() {
            this.isClose = true;
            return Future.succeededFuture();
        }

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public BaseReadStream exceptionHandler(@Nullable Handler<Throwable> var1) {
            this.exceptionHandler = var1;
            return this;
        }

        @Override

        public BaseReadStream handler(@Nullable Handler<Buffer> var1) {
            this.handler = var1;
            return this;
        }

        @Override
        public BaseReadStream endHandler(@Nullable Handler<Void> var1) {
            this.endHandler = var1;
            return this;
        }
    }

    class FileReadStream implements BaseReadStream {
        String path;
        Vertx vertx;
        private Long offset;
        private Long length;
        private Long size;
        private Handler<Buffer> handler;
        private Handler<Throwable> exceptionHandler;
        private Handler<Void> endHandler;
        private boolean isClose;
        private AsyncFile asyncFile;

        public FileReadStream(Vertx vertx, String path) {
            this.path = path;
            this.vertx = vertx;
            this.isClose = false;
        }

        public FileReadStream(Vertx vertx, String path, Long offset, Long length, Long size) {
            this.path = path;
            this.vertx = vertx;
            this.offset = offset;
            this.length = length;
            this.size = size;
            this.isClose = false;
        }

        @Override
        public Future<Void> read() {
            return vertx.fileSystem().open(path, new OpenOptions().setRead(true))
                    .compose(h -> {
                        this.asyncFile = h;

                        if (offset != null && size != null) {
                            // range 读取：设置起始位置和读取总长度
                            h.setReadPos(offset);
                            h.setReadLength(size - offset); // size是结束位置，所以总长= size - offset
                        }

                        // length 作为每次读取的 chunk size
                        if (length != null) {
                            h.setReadBufferSize(length.intValue());
                        }

                        h.handler(this.handler);
                        h.endHandler(this.endHandler);
                        h.exceptionHandler(this.exceptionHandler);
                        // AsyncFile 默认是 paused 状态，需要 resume 触发读取
                        h.resume();
                        return Future.succeededFuture();
                    });
        }

        @Override
        public Future<Void> close() {
            this.isClose = true;
            this.handler = null;
            this.exceptionHandler = null;
            this.endHandler = null;
            if (this.asyncFile != null) {
                Future<Void> result = this.asyncFile.close();
                this.asyncFile = null;
                return result;
            }
            return Future.succeededFuture();
        }

        @Override
        public void pause() {
            if (this.asyncFile != null) {
                this.asyncFile.pause();
            }
        }

        @Override
        public void resume() {
            if (this.asyncFile != null) {
                this.asyncFile.resume();
            }
        }

        @Override
        public BaseReadStream exceptionHandler(@Nullable Handler<Throwable> var1) {
            this.exceptionHandler = var1;
            return this;
        }

        @Override
        public BaseReadStream handler(@Nullable Handler<Buffer> var1) {
            this.handler = var1;
            return this;
        }

        @Override
        public BaseReadStream endHandler(@Nullable Handler<Void> var1) {
            this.endHandler = var1;
            return this;
        }
    }

    /**
     * 下载文件
     *
     * @param loId     大对象id
     * @param offset   偏移量
     * @param length   下载长度
     * @param size     文件大小
     * @param function 处理数据函数
     * @return 数据
     */
    public Future<RowSet<Row>> downloadFile(Long loId, Long offset, Long length, Long size, Function<Row, Future<Void>> function) {
        return getLargeObject(loId, offset, offset + length > size ? size - offset : length)
                .compose(ok -> function.apply(ok.iterator().next()).compose(e -> {
                    long nextOffset = offset + length;
                    if (nextOffset < size) {
                        return downloadFile(loId, nextOffset, length, size, function);
                    }
                    return Future.succeededFuture();
                }));
    }

    public BaseReadStream downloadFile(Vertx vertx, FileEntity fileEntity) {
        if (dbType == SQLDialect.POSTGRESQL) {
            return new PgsqlReadStream(fileEntity.getLoId(), 0L, 1024 * 64L, fileEntity.getSize());
        } else {
            return new FileReadStream(vertx, fileEntity.getPath());
        }
    }

    /**
     * @param vertx
     * @param fileEntity
     * @param offset     偏移量 start
     * @param size       结束。end
     * @return
     */
    public BaseReadStream downloadFile(Vertx vertx, FileEntity fileEntity, Long offset, Long size) {
        if (dbType == SQLDialect.POSTGRESQL) {
            return new PgsqlReadStream(fileEntity.getLoId(), offset, 1024 * 64L, Math.min(fileEntity.getSize(), size));
        } else {
            return new FileReadStream(vertx, fileEntity.getPath(), offset, 1024 * 64L, Math.min(fileEntity.getSize(), size));
        }
    }

    /**
     * 上传文件到存储并保存元数据（公共方法）
     *
     * @param fileName 文件名
     * @param size     文件大小
     * @param refType  资源类型（可为null）
     * @param ref      资源标识（可为null）
     * @param file     本地文件
     * @return FileEntity 异步任务
     */
    public Future<FileEntity> upload(String fileName, long size, String refType, String ref, File file) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setId(UUID.randomUUID());
        fileEntity.setSize(size);
        fileEntity.setFileName(fileName);
        fileEntity.setCreateTime(java.time.LocalDateTime.now());
        fileEntity.setUpdateTime(java.time.LocalDateTime.now());
        fileEntity.setMeta(new io.vertx.core.json.JsonObject());
        fileEntity.setRefType(refType);
        fileEntity.setRef(ref);

        if (dbType == SQLDialect.POSTGRESQL) {
            return save(fileEntity, file, 1024 * 1024).map(fileEntity);
        } else {
            return vertx.executeBlocking(() -> {
                String sha256 = CommonUtils.getSHA256(file);
                fileEntity.setSha256Hash(sha256);
                return sha256;
            }, false).compose(sha256 -> search(
                    field(FileEntity::getSha256Hash).eq(sha256),
                    Map.of()
            ).compose(rows -> vertx.executeBlocking(() -> {
                if (rows.size() == 0) {
                    java.nio.file.Path ossPath = CommonUtils.getOssPath();
                    copyToOss(fileEntity, file.getAbsolutePath(), ossPath);
                } else {
                    String path = rows.iterator().next().getPath();
                    if (org.apache.commons.lang3.StringUtils.isEmpty(path)) {
                        java.nio.file.Path ossPath = CommonUtils.getOssPath();
                        copyToOss(fileEntity, file.getAbsolutePath(), ossPath);
                    } else {
                        java.nio.file.Path target = java.nio.file.Paths.get(path);
                        if (!java.nio.file.Files.exists(target)) {
                            copyToOss(fileEntity, file.getAbsolutePath(), target);
                        } else {
                            fileEntity.setPath(path);
                        }
                    }
                }
                return fileEntity;
            }, false)).compose(entity -> save(entity).map(entity)));
        }
    }

    private void copyToOss(FileEntity fileEntity, String sourcePath, java.nio.file.Path targetPath) throws java.io.IOException {
        if (!java.nio.file.Files.exists(targetPath.getParent())) {
            java.nio.file.Files.createDirectories(targetPath.getParent());
        }
        fileEntity.setPath(targetPath.toString());
        java.nio.file.Files.copy(java.nio.file.Paths.get(sourcePath), targetPath);
    }

    /**
     * 插入
     *
     * @param fileEntity 文件对象
     * @param file       文件
     * @return 异步任务
     */
    public Future<SqlResult<Void>> save(FileEntity fileEntity, File file) {
        return save(fileEntity, file, 1024 * 64);
    }

    /**
     * 插入
     *
     * @param fileEntity 文件对象
     * @param file       文件
     * @param capacity   每次上传块的大小
     * @return 异步任务
     */
    public Future<SqlResult<Void>> save(FileEntity fileEntity, File file, int capacity) {
        return vertx.executeBlocking(() -> {
            String sha256 = CommonUtils.getSHA256(file);
            fileEntity.setSha256Hash(sha256);
            return sha256;
        }, false).compose(sha256 -> {
            return search(field(FileEntity::getSha256Hash).eq(sha256)
                    , Map.of( ))
                    .compose(rows -> {
                        if (rows.size() == 0) {
                            return SqlTemplate.forQuery(client, "SELECT lo_creat(-1)::int8 as lo_id;").execute(Map.of()).compose(loId -> {
                                Row next = loId.iterator().next();
                                Long lo_id = next.getLong("lo_id");
                                fileEntity.setLoId(lo_id);
                                return uploadLargeObject(file, lo_id, capacity);
                            }).compose(ok -> save(fileEntity));

                        } else {
                            FileEntity next = rows.iterator().next();
                            fileEntity.setLoId(next.getLoId());
                            return save(fileEntity);
                        }
                    });
        });
    }

    record ChunkResult(int length, Buffer data) {
    }

    /**
     * 上传大对象块
     *
     * @param channel 文件channel
     * @param buffer  buffer对象
     * @param loId    大对象id
     * @param offset  偏移量
     * @return 异步任务
     */
    public Future<RowSet<Void>> uploadLargeObjectChunk(FileChannel channel, ByteBuffer buffer, Long loId, Long offset) {
        return vertx.<ChunkResult>executeBlocking(() -> {
                    int length = channel.read(buffer);
                    if (length == -1) return new ChunkResult(-1, null);
                    buffer.flip();
                    Buffer data = Buffer.buffer(buffer.array());
                    buffer.clear();
                    return new ChunkResult(length, data);
                }, false)
                .compose(chunk -> {
                    if (chunk.length() == -1) return Future.succeededFuture();
                    return SqlTemplate.forUpdate(client,
                                    "SELECT lo_put(" + loId + "::oid," + offset + "::bigint,#{data})::VARCHAR;")
                            .execute(Map.of("data", chunk.data()))
                            .compose(r -> uploadLargeObjectChunk(channel, buffer, loId, offset + chunk.length()));
                });

    }

    /**
     * 上传大文件到pgsql
     *
     * @param file     文件
     * @param lo_id    大对象id
     * @param capacity 每次上传多少
     * @return 异步任务
     */
    public Future<RowSet<Void>> uploadLargeObject(File file, Long lo_id, Integer capacity) {
        return vertx.executeBlocking(() ->
                        FileChannel.open(Path.of(file.toURI())), false)
                .compose(channel ->
                        uploadLargeObjectChunk(channel, ByteBuffer.allocate(capacity), lo_id, 0L)
                                .compose(e -> vertx.executeBlocking(() -> {
                                    channel.close();
                                    return null;
                                }, false))
                );
    }
}
