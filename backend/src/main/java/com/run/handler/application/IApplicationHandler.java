package com.run.handler.application;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/7/13  22:51}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IApplicationHandler {
    /**
     * 修改
     *
     * @param context 上下文
     */
    void edit(RoutingContext context);

    /**
     * 查询一个
     *
     * @param context 上下文
     */
    void get(RoutingContext context);

    /**
     * 对话
     *
     * @param context 上下文
     */
    void chat(RoutingContext context);

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
    void pageConversationRecord(RoutingContext context);
}
