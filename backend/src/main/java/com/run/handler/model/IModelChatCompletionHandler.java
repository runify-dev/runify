package com.run.handler.model;

import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/7/15}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IModelChatCompletionHandler {
    /**
     * 带 tools 的聊天补全代理（SSE 流式）
     * 供前端 agent loop 调用：前端维护 messages 数组与工具执行，本接口只负责一轮 LLM 补全
     *
     * @param context 上下文
     */
    void completion(RoutingContext context);
}
