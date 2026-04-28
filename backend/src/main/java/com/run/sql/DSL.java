package com.run.sql;

import com.run.sql.condition.Condition;
import com.run.sql.condition.NoCondition;
import com.run.sql.condition.RawCondition;
import com.run.sql.dialect.Dialect;
import com.run.sql.dialect.DialectRegistry;
import com.run.sql.lambda.SerializableFunction;
import com.run.sql.metadata.ModelResolver;
import com.run.sql.model.*;
import com.run.sql.render.RenderSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class DSL {

    private DSL() {
    }

    public static DSLContext using(Dialect dialect) {
        return new DSLContext(dialect, RenderSettings.defaults());
    }

    public static DSLContext using(String dialectName) {
        return using(DialectRegistry.get(dialectName));
    }

    public static DSLContext using(Dialect dialect, RenderSettings settings) {
        return new DSLContext(dialect, settings);
    }

    public static Table table(String name) {
        return Table.of(name);
    }

    public static Table table(Class<?> modelClass) {
        return Table.of(modelClass);
    }

    public static Table table(Object model) {
        return Table.of(ModelResolver.resolve(model).table());
    }

    /**
     * 两参兼容：
     * MySQL / SQLite 视为 catalog + name；PG / Kingbase / H2 / Oracle / DM 视为 schema + name。
     */
    public static Table table(String catalogOrSchema, String name) {
        return Table.of(catalogOrSchema, name);
    }

    public static Table table(String catalog, String schema, String name) {
        return Table.of(catalog, schema, name);
    }

    public static Table rawTable(String expression) {
        return Table.raw(expression);
    }

    public static Table mysqlTable(String catalog, String name) {
        return Table.of(catalog, null, name);
    }

    public static Table schemaTable(String schema, String name) {
        return Table.of(null, schema, name);
    }

    public static <T> Field<T> field(String name) {
        return Field.of(name);
    }

    public static <T, R> Field<R> field(SerializableFunction<T, R> getter) {
        return Field.of(getter);
    }

    public static <T> Field<T> field(String table, String name) {
        return Field.qualified(null, null, table, name);
    }

    public static <T> Field<T> field(String catalog, String schema, String table, String name) {
        return Field.qualified(catalog, schema, table, name);
    }

    public static <T, R> Field<R> field(String catalog, String schema, String table, SerializableFunction<T, R> getter) {
        return Field.qualified(catalog, schema, table, getter);
    }

    public static <T> Field<T> rawField(String expression) {
        return Field.raw(expression);
    }

    public static Field<Object> asterisk() {
        return rawField("*");
    }

    public static Field<Long> count() {
        return rawField("count(*)");
    }

    public static Field<Long> count(Field<?> field) {
        return Field.expression("count", ctx -> "count(" + field.renderRef(ctx) + ")");
    }

    public static Field<String> concat(Value<?>... values) {
        return concat(Arrays.asList(values));
    }

    public static Field<String> concat(List<? extends Value<?>> values) {
        return Field.expression("concat", ctx -> ctx.dialect().functions().concat(ctx, values));
    }

    public static Field<Object> currentTimestamp() {
        return Field.expression("current_timestamp", ctx -> ctx.dialect().functions().currentTimestamp(ctx));
    }

    public static Field<String> jsonText(Field<?> jsonField, String path) {
        return Field.expression("json_text", ctx -> ctx.dialect().json().text(ctx, jsonField, path));
    }

    public static <T> Param<T> param(String name, T value) {
        return Param.of(name, value);
    }

    public static <M, T> Param<T> param(SerializableFunction<M, T> getter, T value) {
        return Param.of(getter, value);
    }

    public static Param<Object> param(String name) {
        return Param.named(name);
    }

    public static <M, T> Param<T> param(SerializableFunction<M, T> getter) {
        return Param.named(getter);
    }

    public static <T> Val<T> val(T value) {
        return Val.of(value);
    }

    public static <T> Inline<T> inline(T value) {
        return Inline.of(value);
    }

    public static Condition noCondition() {
        return NoCondition.INSTANCE;
    }

    public static Condition condition(String sqlTemplate, Map<String, Object> params) {
        return new RawCondition(sqlTemplate, params);
    }
}
