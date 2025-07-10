package com.run.handler.node;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/6  16:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface INodeHandler {
    /**
     * 获取节点列表
     */
    void list(RoutingContext context);

    /**
     * 创建节点
     */
    void create(RoutingContext context);

    /**
     * 修改节点
     */
    void edit(RoutingContext context);

    /**
     * 删除节点 根据节点id
     */
    void delete(RoutingContext context);

}
