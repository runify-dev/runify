package com.run.datasources;

import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Map;

/**
 * 数据源供应商基础接口
 */
public interface IDatasourceProvider {

    /**
     * 获取供应商信息
     */
    DatasourceProviderInfo info();

    /**
     * 获取凭证配置
     */
    BaseDatasourceCredential getCredential();

    /**
     * 获取表单定义列表
     */
    default List<Map<String, Object>> getFormList() {
        return getCredential().toFormList(Map.of());
    }

    /**
     * 验证连接配置
     */
    default void validateCredential(Map<String, Object> credential) {
        getCredential().validateCredential(credential);
    }

    /**
     * 验证连接是否可用
     */
    Future<Boolean> validate(Datasource datasource, Vertx vertx);
}
