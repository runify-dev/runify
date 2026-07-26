package com.run.handler.project;

import io.vertx.ext.web.RoutingContext;

/**
 * 项目级 AI 生成的持久化 handler：蓝图 / 会话 / 任务台账 / 消息流 的读写。
 * agent 仍由前端驱动，本层只做存取与恢复支撑，不承载生成逻辑。
 */
public interface IProjectAiHandler {
    // L1 蓝图（1:1 project）
    void getBlueprint(RoutingContext context);

    void upsertBlueprint(RoutingContext context);

    // L2 会话（1:N project）
    void createSession(RoutingContext context);

    void listSessions(RoutingContext context);

    void getSession(RoutingContext context);

    void updateSession(RoutingContext context);

    void deleteSession(RoutingContext context);

    // L3 任务台账（1:N session）
    void createTask(RoutingContext context);

    void listTasks(RoutingContext context);

    void getTask(RoutingContext context);

    void updateTask(RoutingContext context);

    // 统一消息流（append-only）
    void appendMessage(RoutingContext context);

    void listMessages(RoutingContext context);
}
