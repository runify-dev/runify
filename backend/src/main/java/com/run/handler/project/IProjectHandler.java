package com.run.handler.project;

import com.run.dao.entity.Project;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2025/12/20  18:46}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public interface IProjectHandler extends IResourceHandler<Project> {
    /**
     * 获取项目统一异常配置
     */
    void getErrorResponse(RoutingContext context);

    /**
     * 修改项目统一异常配置
     */
    void editErrorResponse(RoutingContext context);
}
