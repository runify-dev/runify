package com.run.handler.common;

import com.run.handler.common.pojo.QueryFolderPojo;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/9/9  23:49}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface ITreeHandler<F, R> {
    /**
     * 获取文件夹下资源列表
     */
    void list(RoutingContext context);

    /**
     * 获取目录下所有资源列表
     *
     * @param context 上下文
     */
    void listResource(RoutingContext context);

    /**
     * 获取所有数据
     *
     * @param context 上下文
     */
    void listTree(RoutingContext context);

    /**
     * 获取当前用户收藏列表
     *
     * @param context 上下文
     */
    void listStar(RoutingContext context);

    /**
     * 获取当前用户分享列表
     *
     * @param context 上下文
     */
    void listShared(RoutingContext context);


    /**
     * 删除节点 根据节点id
     */
    void delete(RoutingContext context);

    void rename(RoutingContext context);

    void get(RoutingContext context);

    void create(RoutingContext context);
}
