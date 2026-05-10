package com.run.handler.datasource;

import com.run.dao.entity.Datasource;
import com.run.handler.common.IResourceHandler;
import io.vertx.ext.web.RoutingContext;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/1  22:30}
 * {@code @Version 1.0}
 * {@code @注释: 数据源连接池处理器}
 */
public interface IDataSourceHandler extends IResourceHandler<Datasource> {
    /**
     * 修改数据源
     */
    void edit(RoutingContext context);

    /**
     * 获取数据源类型列表
     */
    void getDataSourceTypes(RoutingContext context);

    /**
     * 获取供应商列表（按类型过滤）
     */
    void getProviders(RoutingContext context);

    /**
     * 获取供应商的表单定义
     */
    void getFormDefinition(RoutingContext context);

    /**
     * 获取数据源的表列表
     */
    void getTables(RoutingContext context);

    /**
     * 获取表的列信息
     */
    void getColumns(RoutingContext context);
}
