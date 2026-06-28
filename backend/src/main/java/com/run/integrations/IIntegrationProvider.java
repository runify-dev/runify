package com.run.integrations;

import com.run.dao.entity.Integration;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 第三方集成供应商接口。每个平台(企业微信应用/企业微信机器人/飞书/微信...)独立实现, 互不交叉。 }
 */
public interface IIntegrationProvider {

    /**
     * 供应商信息
     */
    IntegrationProviderInfo info();

    /**
     * 回调 URL 验证(平台侧 GET 握手)
     */
    void verify(RoutingContext context, Integration integration, IntegrationDeps deps);

    /**
     * 入站消息回调(平台侧 POST)
     */
    void handleMessage(RoutingContext context, Integration integration, IntegrationDeps deps);
}
