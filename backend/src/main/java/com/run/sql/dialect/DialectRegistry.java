package com.run.sql.dialect;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for dialect extension.
 */
public final class DialectRegistry {

    private static final Map<String, Dialect> DIALECTS = new ConcurrentHashMap<>();

    static {
        register(BuiltInDialects.MYSQL);
        register(BuiltInDialects.POSTGRESQL);
        register(BuiltInDialects.KINGBASE);
        register(BuiltInDialects.SQLITE);
        register(BuiltInDialects.H2);
        register(BuiltInDialects.ORACLE);
        register(BuiltInDialects.DM);
    }

    private DialectRegistry() {
    }

    public static void register(Dialect dialect) {
        Objects.requireNonNull(dialect, "dialect");
        DIALECTS.put(normalize(dialect.dialectName()), dialect);
    }

    public static Dialect get(String name) {
        Dialect dialect = DIALECTS.get(normalize(name));
        if (dialect == null) {
            throw new IllegalArgumentException("Unknown SQL dialect: " + name + ", registered=" + DIALECTS.keySet());
        }
        return dialect;
    }

    public static Map<String, Dialect> all() {
        return Map.copyOf(DIALECTS);
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("dialect name is blank");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
