package com.run.datasources.impl.mysql;

import com.run.common.util.CommonUtils;
import com.run.datasources.BaseDatasourceCredential;
import com.run.datasources.DatasourceProviderInfo;
import com.run.datasources.DataSourceType;
import com.run.datasources.SqlProvider;
import com.run.datasources.impl.mysql.credential.MySQLCredential;
import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 数据源供应商实现
 */
public class MySQLProvider implements SqlProvider {

    private static final DatasourceProviderInfo info;
    private static final MySQLCredential credential;

    static {
        String icon = CommonUtils.getFileContent("com/run/datasources/impl/mysql/icon/mysql.svg");
        info = new DatasourceProviderInfo("mysql", "MySQL", icon, DataSourceType.SQL);
        credential = new MySQLCredential();
    }

    @Override
    public DatasourceProviderInfo info() {
        return info;
    }

    @Override
    public BaseDatasourceCredential getCredential() {
        return credential;
    }

    @Override
    public Pool createPool(Datasource datasource, Vertx vertx) {
        JsonObject collection = datasource.decrypt();
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(collection.getInteger("port"))
                .setHost(collection.getString("host"))
                .setDatabase(collection.getString("database"))
                .setUser(collection.getString("user"))
                .setPassword(collection.getString("password"));
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(collection.getInteger("maxSize"));
        return MySQLBuilder
                .pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(vertx)
                .build();
    }

    @Override
    public Future<Boolean> validate(Datasource datasource, Vertx vertx) {
        Pool p = createPool(datasource, vertx);
        return p.query("SELECT 1;")
                .execute()
                .compose(_ -> Future.succeededFuture(true));
    }

    @Override
    public Future<List<Map<String, Object>>> getTables(Pool pool) {
        String query = """
                SELECT table_name, engine, table_comment
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                ORDER BY table_name
                """;
        return pool.query(query).execute().map(rows -> {
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> table = new HashMap<>();
                table.put("name", row.getString("table_name"));
                table.put("engine", row.getString("engine"));
                table.put("comment", row.getString("table_comment"));
                tables.add(table);
            }
            return tables;
        });
    }

    @Override
    public Future<List<Map<String, Object>>> getColumns(Pool pool, String tableName) {
        String query = """
                SELECT column_name, data_type, is_nullable, column_key, column_comment
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                ORDER BY ordinal_position
                """;
        return pool.preparedQuery(query).execute(Tuple.of(tableName)).map(rows -> {
            List<Map<String, Object>> columns = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", row.getString("column_name"));
                column.put("type", row.getString("data_type"));
                column.put("nullable", "YES".equals(row.getString("is_nullable")));
                column.put("primaryKey", "PRI".equals(row.getString("column_key")));
                column.put("comment", row.getString("column_comment"));
                columns.add(column);
            }
            return columns;
        });
    }
}
