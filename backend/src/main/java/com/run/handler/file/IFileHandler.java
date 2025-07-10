package com.run.handler.file;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/30  22:42}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IFileHandler {
    /**
     * 上传文件
     *
     * @param context 上下文
     */
    void upload(RoutingContext context);

    /**
     * 下载文件
     *
     * @param context 上下文
     */
    void download(RoutingContext context);
}
