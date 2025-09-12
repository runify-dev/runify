package com.run.handler.user;


import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/4/5  16:52}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IUserHandler {
    /**
     * 创建用户
     *
     * @return 创建用户
     */
    Handler<RoutingContext> createUser();

    /**
     * 登陆
     *
     * @return 登陆处理器
     */
    Handler<RoutingContext> login();

    /**
     * 获取用户信息
     *
     * @return 获取用户信息
     */
    Handler<RoutingContext> profile();

    /**
     * 登出
     *
     * @return 登出处理器
     */
    Handler<RoutingContext> logout();

    void page(RoutingContext context);
}
