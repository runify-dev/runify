package com.run.datasources.impl.postgresql;

import com.run.common.util.CommonUtils;
import com.run.datasources.BaseDatasourceCredential;
import com.run.datasources.DatasourceProviderInfo;
import com.run.datasources.DataSourceType;
import com.run.datasources.SqlProvider;
import com.run.datasources.impl.postgresql.credential.PostgreSQLCredential;
import com.run.dao.entity.Datasource;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 数据源供应商实现
 */
public class PostgreSQLProvider implements SqlProvider {

    private static final DatasourceProviderInfo info;
    private static final PostgreSQLCredential credential;

    static {
        String icon = CommonUtils.getFileContent("com/run/datasources/impl/postgresql/icon/postgresql.svg");
        info = new DatasourceProviderInfo("postgresql", "PostgreSQL", icon, DataSourceType.SQL);
        credential = new PostgreSQLCredential();
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
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(collection.getInteger("port"))
                .setHost(collection.getString("host"))
                .setDatabase(collection.getString("database"))
                .setUser(collection.getString("user"))
                .setPassword(collection.getString("password"));
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(collection.getInteger("maxSize"));
        return PgBuilder
                .pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(vertx)
                .build();
    }

    @Override
    public Future<Boolean> validate(Datasource pool, Vertx vertx) {
        Pool p = createPool(pool, vertx);
        return p.query("SELECT 1;")
                .execute()
                .compose(_ -> Future.succeededFuture(true));
    }

    @Override
    public Future<List<Map<String, Object>>> getTables(Pool pool) {
        String query = """
                SELECT
                    t.table_name,
                    t.table_type,
                    obj_description((t.table_schema || '.' || t.table_name)::regclass, 'pg_class') AS table_comment
                FROM information_schema.tables t
                WHERE t.table_schema = 'public'
                ORDER BY t.table_name
                """;
        return pool.query(query).execute().map(rows -> {
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> table = new HashMap<>();
                table.put("name", row.getString("table_name"));
                table.put("engine", row.getString("table_type"));
                table.put("comment", row.getString("table_comment"));
                tables.add(table);
            }
            return tables;
        });
    }

    @Override
    public Future<List<Map<String, Object>>> getColumns(Pool pool, String tableName) {
        String query = """
                SELECT
                    c.column_name,
                    c.data_type,
                    c.is_nullable,
                    CASE WHEN pk.column_name IS NOT NULL THEN true ELSE false END AS is_primary_key,
                    col_description((c.table_schema || '.' || c.table_name)::regclass, c.ordinal_position) AS column_comment
                FROM information_schema.columns c
                LEFT JOIN (
                    SELECT ku.column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage ku ON tc.constraint_name = ku.constraint_name
                    WHERE tc.table_schema = 'public' AND tc.table_name = $1 AND tc.constraint_type = 'PRIMARY KEY'
                ) pk ON c.column_name = pk.column_name
                WHERE c.table_schema = 'public' AND c.table_name = $1
                ORDER BY c.ordinal_position
                """;
        return pool.preparedQuery(query).execute(io.vertx.sqlclient.Tuple.of(tableName)).map(rows -> {
            List<Map<String, Object>> columns = new ArrayList<>();
            for (Row row : rows) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", row.getString("column_name"));
                column.put("type", row.getString("data_type"));
                column.put("nullable", "YES".equals(row.getString("is_nullable")));
                column.put("primaryKey", row.getBoolean("is_primary_key"));
                column.put("comment", row.getString("column_comment"));
                columns.add(column);
            }
            return columns;
        });
    }
}
