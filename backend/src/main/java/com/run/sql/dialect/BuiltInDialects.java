package com.run.sql.dialect;

/** Built-in dialect instances. */
public final class BuiltInDialects {

    private BuiltInDialects() {
    }

    public static final Dialect MYSQL = new MySQLDialect();
    public static final Dialect POSTGRESQL = new PostgreSQLDialect();
    public static final Dialect KINGBASE = new KingbaseDialect();
    public static final Dialect SQLITE = new SQLiteDialect();
    public static final Dialect H2 = new H2Dialect();
    public static final Dialect ORACLE = new OracleStyleDialect("oracle");
    public static final Dialect DM = new OracleStyleDialect("dm");
}
