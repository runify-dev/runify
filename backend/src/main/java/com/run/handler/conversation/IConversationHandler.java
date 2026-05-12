package com.run.handler.conversation;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/4/9  22:08}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IConversationHandler {
    /**
     * 重命名对话
     *
     * @param context
     */
    void modifyName(RoutingContext context);

    /**
     * 获取当前对话信息
     *
     * @param context
     */
    void config(RoutingContext context);

    /**
     * 创建对话
     *
     * @param context
     */
    void createConversation(RoutingContext context);

    /**
     * 对话
     *
     * @param context
     */
    void conversation(RoutingContext context);

    /**
     * 匿名登陆
     *
     * @param context
     */
    void anonymousLogin(RoutingContext context);

    /**
     * 删除对话
     *
     * @param context
     */
    void delConversation(RoutingContext context);

    /**
     * 分页获取对话列表
     *
     * @param context
     */
    void pageConversation(RoutingContext context);

    /**
     * 分页获取 Message
     *
     * @param context
     */
    void pageMessage(RoutingContext context);

    void resumeStream(RoutingContext context);

    void statusStream(RoutingContext context);

    void cancel(RoutingContext routingContext);
}
