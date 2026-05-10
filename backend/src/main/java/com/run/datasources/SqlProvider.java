package com.run.datasources;

import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

import java.util.List;
import java.util.Map;

/**
 * SQL 数据源供应商接口
 */
public interface SqlProvider extends IDatasourceProvider {

    /**
     * 创建数据库连接池
     */
    Pool createPool(Datasource datasource, Vertx vertx);

    /**
     * 获取表列表
     */
    Future<List<Map<String, Object>>> getTables(Pool pool);

    /**
     * 获取表的列信息
     */
    Future<List<Map<String, Object>>> getColumns(Pool pool, String tableName);
}
