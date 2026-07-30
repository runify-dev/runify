package com.run.handler.project;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/21  18:57}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IProcessorHandler {
    void create(RoutingContext context);

    void page(RoutingContext context);

    void get(RoutingContext context);

    void edit(RoutingContext context);

    /**
     * 发布：把当前画布工作流(草稿)+ meta 快照为一个新版本;部署时端点用最新已发布版本
     *
     * @param context 上下文
     */
    void publish(RoutingContext context);

    /**
     * 发布历史列表
     *
     * @param context 上下文
     */
    void listVersions(RoutingContext context);

    /**
     * 取单个发布版本(含 snapshot,供前端回滚回填画布)
     *
     * @param context 上下文
     */
    void getVersion(RoutingContext context);

    void deploy(RoutingContext context);

    /**
     * 取消部署
     *
     * @param context 上下文
     */
    void undeploy(RoutingContext context);

    /**
     * 删除处理器（先下线其端点，再删除实体）
     *
     * @param context 上下文
     */
    void delete(RoutingContext context);
}
