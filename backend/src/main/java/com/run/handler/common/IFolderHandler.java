package com.run.handler.common;

import com.run.handler.common.pojo.CreateFolderPojo;
import com.run.handler.common.pojo.QueryFolderPojo;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/10/30  11:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IFolderHandler<F> {
    /**
     * 获取文件夹详情
     *
     * @param folderId 文件夹id
     * @return 文件夹详情
     */
    Future<F> get(String folderId);


    /**
     * 删除文件夹
     *
     * @param folderId 文件夹名称
     * @return 是否删除文件夹
     */
    Future<Boolean> delete(String folderId);

    /**
     * 修改文件夹名称
     *
     * @param folderId 文件夹id
     * @param name     修改后的名称
     * @return 修改后的数据
     */
    Future<F> rename(String folderId, String name);

    /**
     * 创建文件夹
     *
     * @param createFolderPojo 创建文件夹所需数据
     * @return 文件夹对象
     */
    Future<F> create(CreateFolderPojo createFolderPojo);

    /**
     * @param folderId       需要移动的资源
     * @param targetFolderId 需要移动到的位置
     * @return 移动后的数据
     */
    Future<Boolean> move(String folderId, String targetFolderId);

    void create(RoutingContext context);

    void delete(RoutingContext context);

    void rename(RoutingContext context);

    void move(RoutingContext context);

    void get(RoutingContext context);

}
