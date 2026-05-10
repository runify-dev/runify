package com.run.datasources;

import com.run.datasources.impl.localcache.LocalCacheProvider;
import com.run.datasources.impl.mysql.MySQLProvider;
import com.run.datasources.impl.postgresql.PostgreSQLProvider;
import com.run.datasources.impl.redis.RedisProvider;
import lombok.Getter;

/**
 * 数据源供应商枚举
 */
@Getter
public enum DatasourceProviderConstants {

    postgresql(new PostgreSQLProvider()),
    mysql(new MySQLProvider()),
    redis(new RedisProvider()),
    local_cache(new LocalCacheProvider());

    DatasourceProviderConstants(IDatasourceProvider provider) {
        this.provider = provider;
    }

    private final IDatasourceProvider provider;
}
