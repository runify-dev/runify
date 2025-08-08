package com.run.handler.model;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/5  22:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IModelHandler {
    /**
     * 获取供应商列表
     *
     * @param context 上下文
     */
    void getProvider(RoutingContext context);

    /**
     * 获取模型列表
     *
     * @param context 上下文
     */
    void getModelList(RoutingContext context);

    /**
     * 获取模型类型列表
     *
     * @param context 上下文
     */
    void listModelType(RoutingContext context);

    void edit(RoutingContext context);
}
