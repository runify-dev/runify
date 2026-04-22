package com.run.handler.common;

import com.run.handler.common.pojo.QueryResourcePojo;
import com.run.handler.common.pojo.SimpleNodePojo;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.UUID;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  13:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IResourceHandler<R> {

    Future<R> get(String resourceId);

    /**
     * 获取列表
     *
     * @param query 查询条件
     * @return 列表
     */
    Future<List<R>> list(QueryResourcePojo query);

    /**
     * 获取列表
     *
     * @param query 查询条件
     * @return 列表
     */
    Future<List<R>> listByPermission(QueryResourcePojo query, UUID userId,Boolean resourceRead);

    /**
     * 获取扁平树形数据
     *
     * @param query 查询
     * @return 资源和文件夹数据
     */
    Future<List<SimpleNodePojo>> tree(QueryResourcePojo query);

    /**
     * 获取扁平树形数据
     *
     * @param query 查询
     * @return 资源和文件夹数据
     */
    Future<List<SimpleNodePojo>> treeByPermission(QueryResourcePojo query, UUID userId,Boolean resourceRead);

    /**
     * 删除资源
     *
     * @param resourceId 文件夹名称
     * @return 是否删除成功
     */
    Future<Boolean> delete(String resourceId);

    /**
     * 修改资源名称
     *
     * @param resourceId 文件夹id
     * @param name       修改后的名称
     * @return 修改后的数据
     */
    Future<R> rename(String resourceId, String name);

    /**
     * @param resourceId 需要移动的资源
     * @param folderId   需要移动到的位置
     * @return 移动后的数据
     */
    Future<Boolean> move(String resourceId, String folderId);

    void list(RoutingContext context);

    void tree(RoutingContext context);

    void delete(RoutingContext context);

    void rename(RoutingContext context);

    void move(RoutingContext context);

    void get(RoutingContext context);

    void create(RoutingContext context);

    void listResourcePermission(RoutingContext context);

    void authResourcePermission(RoutingContext context);
}
