package com.run.handler.role;

import io.vertx.ext.web.RoutingContext;

public interface IRoleHandler {
    /**
     * 获取Role list Page or list
     *
     * @param context 上下文
     */
    void query(RoutingContext context);

    /**
     * 创建 角色
     *
     * @param context
     */
    void create(RoutingContext context);

    /**
     * 删除
     *
     * @param context
     */
    void delete(RoutingContext context);

    /**
     * 添加用户
     *
     * @param context
     */
    void addUser(RoutingContext context);

    void permissions(RoutingContext context);

    void modifyPermissions(RoutingContext context);
}
