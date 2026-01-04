package com.run.common.project.pool;

import com.run.common.config.DataBase;
import com.run.dao.entity.DatabaseConnectionPool;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2026/1/2  14:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class PostgreSQL {
    public static Pool toPool(DatabaseConnectionPool databaseConnectionPool, Vertx vertx) {
        JsonObject meta = databaseConnectionPool.getMeta();
        JsonObject collection = meta.getJsonObject("collection");
        JsonObject pool = meta.getJsonObject("pool");
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(collection.getInteger("port"))
                .setHost(collection.getString("host"))
                .setDatabase(collection.getString("database"))
                .setUser(collection.getString("user"))
                .setPassword(collection.getString("password"));
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(pool.getInteger("maxSize"));
        return PgBuilder
                .pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(vertx)
                .build();
    }

}
