package com.run.dao.common.entity;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/22  00:25}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface BaseReadStream {


    Future<Void> read();

    Future<Void> close();

    void pause();

    void resume();

    BaseReadStream exceptionHandler(@Nullable Handler<Throwable> var1);


    BaseReadStream handler(@Nullable Handler<Buffer> var1);


    BaseReadStream endHandler(@Nullable Handler<Void> var1);
}
