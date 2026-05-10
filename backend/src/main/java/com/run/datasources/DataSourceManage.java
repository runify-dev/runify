package com.run.datasources;

import com.run.dao.entity.Datasource;
import com.run.dao.mapper.DatasourceMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

public class DataSourceManage {
    private static final ConcurrentMap<UUID, Pool> poolMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, SimpleCache> cacheMap = new ConcurrentHashMap<>();

    public static Pool getPool(Datasource datasource, Vertx vertx) {
        return poolMap.computeIfAbsent(datasource.getId(), _ -> {
            SqlProvider sqlProvider = (SqlProvider) datasource.getProvider().getProvider();
            return sqlProvider.createPool(datasource, vertx);
        });
    }

    public static SimpleCache getCache(Datasource datasource, Vertx vertx) {
        return cacheMap.computeIfAbsent(datasource.getId(), _ -> {
            CacheProvider cacheProvider = (CacheProvider) datasource.getProvider().getProvider();
            return cacheProvider.createCache(datasource, vertx);
        });
    }

    public static Future<Pool> getPoolAsync(UUID datasourceId, BiFunction<UUID, DatasourceMapper, Future<Datasource>> fn, DatasourceMapper mapper, Vertx vertx) {
        Pool cached = poolMap.get(datasourceId);
        if (cached != null) {
            return Future.succeededFuture(cached);
        }
        return fn.apply(datasourceId, mapper).map(datasource -> getPool(datasource, vertx));
    }

    public static Future<SimpleCache> getCacheAsync(UUID datasourceId, BiFunction<UUID, DatasourceMapper, Future<Datasource>> fn, DatasourceMapper mapper, Vertx vertx) {
        SimpleCache cached = cacheMap.get(datasourceId);
        if (cached != null) {
            return Future.succeededFuture(cached);
        }
        return fn.apply(datasourceId, mapper).map(datasource -> getCache(datasource, vertx));
    }

    public static void remove(UUID datasourceId) {
        Pool pool = poolMap.remove(datasourceId);
        if (pool != null) {
            pool.close();
        }
        SimpleCache cache = cacheMap.remove(datasourceId);
        if (cache != null) {
            cache.close();
        }
    }
}
