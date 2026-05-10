package com.run.datasources;

import com.run.dao.entity.Datasource;
import io.vertx.core.Vertx;

/**
 * 缓存数据源供应商接口
 */
public interface CacheProvider extends IDatasourceProvider {

    /**
     * 创建缓存实例
     */
    SimpleCache createCache(Datasource datasource, Vertx vertx);
}
