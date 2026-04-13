package com.run.handler.application;

import com.run.dao.entity.Application;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IApplicationHandler extends IResourceHandler<Application> {
    /**
     * 修改
     *
     * @param context 上下文
     */
    void edit(RoutingContext context);


    /**
     * 对话
     *
     * @param context 上下文
     */
    void chat(RoutingContext context);

    /**
     * 创建对话
     *
     * @param context 上下文
     */
    void createConversation(RoutingContext context);

    /**
     * 分页查询对话
     *
     * @param context 上下文
     */
    void pageConversation(RoutingContext context);

    /**
     * 分页查询对话记录
     *
     * @param context 上下文
     */
    void pageConversationMessage(RoutingContext context);

}
