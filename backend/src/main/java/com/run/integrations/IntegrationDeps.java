package com.run.integrations;

import com.run.dao.mapper.FileMapper;
import com.run.handler.integration.IIntegrationMessageDispatcher;
import io.vertx.core.Vertx;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/6/20  10:00}
 * {@code @Version 1.0}
 * {@code @注释: 供应商实现运行所需的共享依赖(供应商本身是无状态枚举单例, 依赖按调用传入) }
 */
public record IntegrationDeps(Vertx vertx,
                              IIntegrationMessageDispatcher dispatcher,
                              FileMapper fileMapper) {
}
