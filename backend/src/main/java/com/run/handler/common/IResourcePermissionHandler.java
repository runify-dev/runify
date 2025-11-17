package com.run.handler.common;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/11/4  21:39}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IResourcePermissionHandler {
    /**
     * 获取资源权限列表
     * 包裹文件夹和资源 也就是树形列表
     *
     * @param context 获取资源权限列表
     */
    void list(RoutingContext context);

    /**
     * 修改资源权限
     *
     * @param context
     */
    void edit(RoutingContext context);
}
