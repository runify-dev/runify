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
     * 发布：把当前画布工作流(草稿)快照为一个新版本,最新版本即线上生效版本
     *
     * @param context 上下文
     */
    void publish(RoutingContext context);

    /**
     * 发布历史列表
     *
     * @param context 上下文
     */
    void listVersions(RoutingContext context);

    /**
     * 取单个发布版本(含 snapshot,供前端回滚回填画布)
     *
     * @param context 上下文
     */
    void getVersion(RoutingContext context);


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

    /**
     * 会话的上下文记忆（跨对话层）：摘要 + 便签（含产物）
     *
     * @param context 上下文
     */
    void conversationContext(RoutingContext context);

    void listSections(RoutingContext context);

    void saveSections(RoutingContext context);

    /**
     * 后台侧「我的便签」：当前登录管理员作为 user 在该应用沉淀的 user 档便签（调试/管理端对话页用）。
     */
    void mySections(RoutingContext context);

    /**
     * 导出应用
     *
     * @param context 上下文
     */
    void exportApplication(RoutingContext context);

    /**
     * 导入应用
     *
     * @param context 上下文
     */
    void importApplication(RoutingContext context);

    void mineConversation(RoutingContext context);

}
