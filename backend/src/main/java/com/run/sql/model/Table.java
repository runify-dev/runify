package com.run.sql.model;

import com.run.sql.QueryPart;
import com.run.sql.lambda.SerializableFunction;
import com.run.sql.metadata.ModelResolver;
import com.run.sql.metadata.TableMeta;
import com.run.sql.render.RenderContext;

import java.util.Objects;

public final class Table implements QueryPart {

    private final String catalog;
    private final String schema;
    private final String name;
    private final String alias;
    private final boolean raw;

    private Table(String catalog, String schema, String name, String alias, boolean raw) {
        this.catalog = catalog;
        this.schema = schema;
        this.name = Objects.requireNonNull(name, "name");
        this.alias = alias;
        this.raw = raw;
    }

    public static Table of(String name) {
        return new Table(null, null, name, null, false);
    }

    public static Table of(Class<?> modelClass) {
        return of(ModelResolver.resolve(modelClass).table());
    }

    public static Table of(TableMeta meta) {
        Objects.requireNonNull(meta, "meta");
        return new Table(meta.catalog(), meta.schema(), meta.name(), null, false);
    }

    /**
     * 两参兼容：
     * - MySQL / SQLite 会把第一个参数当 catalog/database
     * - PostgreSQL / Kingbase / H2 / Oracle / DM 会把第一个参数当 schema
     */
    public static Table of(String catalogOrSchema, String name) {
        return new Table(catalogOrSchema, catalogOrSchema, name, null, false);
    }

    public static Table of(String catalog, String schema, String name) {
        return new Table(catalog, schema, name, null, false);
    }

    public static Table raw(String expression) {
        return new Table(null, null, expression, null, true);
    }

    public Table as(String alias) {
        return new Table(catalog, schema, name, alias, raw);
    }

    public String catalog() {
        return catalog;
    }

    public String schema() {
        return schema;
    }

    public String name() {
        return name;
    }

    public String alias() {
        return alias;
    }

    public boolean raw() {
        return raw;
    }

    public <T> Field<T> field(String column) {
        if (alias != null && !alias.isBlank()) {
            return Field.qualified(null, null, alias, column);
        }
        return Field.qualified(catalog, schema, name, column);
    }

    public <T, R> Field<R> field(SerializableFunction<T, R> getter) {
        if (alias != null && !alias.isBlank()) {
            return Field.qualified(null, null, alias, getter);
        }
        return Field.qualified(catalog, schema, name, getter);
    }

    @Override
    public String render(RenderContext ctx) {
        String sql = raw ? name : ctx.tableIdentifier(catalog, schema, name);
        if (alias == null || alias.isBlank()) {
            return sql;
        }
        return sql + " " + ctx.identifier(alias);
    }
}
