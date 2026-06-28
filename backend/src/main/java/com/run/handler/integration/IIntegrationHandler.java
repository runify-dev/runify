package com.run.handler.integration;

import com.run.dao.entity.Integration;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/19  21:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IIntegrationHandler extends IResourceHandler<Integration> {

    /**
     * 获取集成详情(凭证脱敏)
     *
     * @param context 上下文
     */
    void get(RoutingContext context);

    /**
     * 编辑集成
     *
     * @param context 上下文
     */
    void edit(RoutingContext context);

    /**
     * 平台类型目录(类型/标签/回调路径/认证方式/凭证字段), 供前端新建集成渲染
     *
     * @param context 上下文
     */
    void getTypes(RoutingContext context);
}
