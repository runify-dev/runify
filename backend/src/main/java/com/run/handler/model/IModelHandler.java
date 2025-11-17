package com.run.handler.model;

import com.run.dao.entity.Knowledge;
import com.run.dao.entity.Model;
import com.run.handler.common.IResourceHandler;
import com.run.handler.common.ITreeHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/8/5  22:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IModelHandler extends IResourceHandler<Model> {
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
