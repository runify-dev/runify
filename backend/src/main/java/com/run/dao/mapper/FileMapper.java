package com.run.dao.mapper;

import com.run.common.config.AppConfig;

import com.run.common.util.CommonUtils;
import com.run.dao.common.F;
import com.run.dao.common.entity.BaseReadStream;
import com.run.dao.common.mapper.BaseMapper;
import com.run.dao.entity.FileEntity;
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
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.jetbrains.annotations.Nullable;
import org.jooq.Field;
import org.jooq.SQLDialect;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/29  20:40}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class FileMapper extends BaseMapper<FileEntity> {
    @Inject
    public FileMapper(Pool client, AppConfig appConfig) {
        super(client, appConfig);
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
        public BaseReadStream exceptionHandler(@org.jetbrains.annotations.Nullable Handler<Throwable> var1) {
            this.exceptionHandler = var1;
            return this;
        }

        @Override

        public BaseReadStream handler(@org.jetbrains.annotations.Nullable Handler<Buffer> var1) {
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
        private Handler<Buffer> handler;
        private Handler<Throwable> exceptionHandler;
        private Handler<Void> endHandler;
        private AsyncFile asyncFile;

        public FileReadStream(Vertx vertx, String path) {
            this.path = path;
            this.vertx = vertx;
        }

        @Override
        public Future<Void> read() {
            return vertx.fileSystem().open(path, new OpenOptions().setRead(true))
                    .compose(h -> {
                        if (this.asyncFile == null) {
                            this.asyncFile = h;
                        }
                        h.handler(this.handler);
                        h.endHandler(this.endHandler);
                        h.exceptionHandler(this.exceptionHandler);

                        return Future.succeededFuture();
                    });
        }

        @Override
        public Future<Void> close() {
            this.asyncFile.close();
            return Future.succeededFuture();
        }

        @Override
        public BaseReadStream exceptionHandler(@org.jetbrains.annotations.Nullable Handler<Throwable> var1) {
            this.exceptionHandler = var1;
            return this;
        }

        @Override

        public BaseReadStream handler(@org.jetbrains.annotations.Nullable Handler<Buffer> var1) {
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
        if (dbType == SQLDialect.POSTGRES) {
            return new PgsqlReadStream(fileEntity.getLoId(), 0L, 1024 * 64L, fileEntity.getSize());
        } else {
            return new FileReadStream(vertx, fileEntity.getPath());
        }


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
        String sha256 = CommonUtils.getSHA256(file);
        fileEntity.setSha256Hash(sha256);

        return search(F.field(FileEntity::getSha256Hash).eq(F.params(FileEntity::getSha256Hash))
                , Map.of("sha256_hash", sha256))
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
        try {
            int length;
            if ((length = channel.read(buffer)) != -1) {
                buffer.flip();
                Buffer data = Buffer.buffer(buffer.array());
                buffer.clear();
                return SqlTemplate.forUpdate(client, "SELECT lo_put(" + loId + "::oid," + offset + "::bigint,#{data})::VARCHAR;").execute(Map.of("data", data)).compose(r -> uploadLargeObjectChunk(channel, buffer, loId, offset + length));
            } else {
                return Future.succeededFuture();
            }
        } catch (IOException e) {
            return Future.failedFuture(e);
        }

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
        try {
            FileChannel channel = FileChannel.open(Path.of(file.toURI()));
            ByteBuffer buffer = ByteBuffer.allocate(capacity);
            return uploadLargeObjectChunk(channel, buffer, lo_id, 0L).compose(e -> {
                try {
                    channel.close();
                    return Future.succeededFuture();
                } catch (IOException ex) {
                    return Future.failedFuture(ex);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
