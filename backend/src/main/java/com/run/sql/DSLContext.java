package com.run.sql;

import com.run.sql.condition.Condition;
import com.run.sql.dialect.Dialect;
import com.run.sql.metadata.ModelResolver;
import com.run.sql.model.Field;
import com.run.sql.model.ModelSetOptions;
import com.run.sql.model.Table;
import com.run.sql.query.DeleteQuery;
import com.run.sql.query.InsertQuery;
import com.run.sql.query.SelectQuery;
import com.run.sql.query.UpdateQuery;
import com.run.sql.render.RenderSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DSLContext {
    private final Dialect dialect;
    private final RenderSettings settings;

    public DSLContext(Dialect dialect, RenderSettings settings) {
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public Dialect dialect() {
        return dialect;
    }

    public RenderSettings settings() {
        return settings;
    }

    public SelectQuery select(Field<?>... fields) {
        return new SelectQuery(dialect, settings, Arrays.asList(fields));
    }

    public SelectQuery select(Collection<Field<?>> fields) {
        return new SelectQuery(dialect, settings, new ArrayList<>(fields));
    }

    public SelectQuery selectOne() {
        return select(DSL.rawField("1"));
    }

    public SelectQuery selectCount() {
        return select(DSL.count());
    }

    public SelectQuery selectCount(Field<?> field) {
        return select(DSL.count(field));
    }

    public SelectQuery selectCountFrom(Table table) {
        return selectCount().from(table);
    }

    public SelectQuery selectCount(Table table) {
        return selectCountFrom(table);
    }

    public SelectQuery selectFrom(Table table) {
        return select(DSL.asterisk()).from(table);
    }

    public InsertQuery insertInto(Table table) {
        return new InsertQuery(dialect, settings, table);
    }

    /**
     * 直接创建 insert 并批量 set 字段值。
     *
     * <pre>{@code
     * Map<Field<?>, Object> values = new LinkedHashMap<>();
     * values.put(field("id"), param("id", 1));
     * values.put(field("name"), "张三");
     *
     * RenderedSql sql = ctx.insertInto(table("user"), values).render();
     * }</pre>
     */
    public InsertQuery insertInto(Table table, Map<? extends Field<?>, ?> values) {
        return insertInto(table).set(values);
    }

    /**
     * 直接创建 insert 并批量 set Value 类型字段值。
     *
     * <p>由于 Java 泛型擦除，不能和 {@link #insertInto(Table, Map)} 使用同名重载，所以这里命名为 insertIntoValues。</p>
     */
    public InsertQuery insertIntoValues(Table table, Map<? extends Field<?>, ? extends Value<?>> values) {
        return insertInto(table).setValues(values);
    }

    /**
     * 根据模型 Class 上的 @Table 注解推导 insert 表。
     *
     * <p>只推导表，不会自动 set 字段值。需要字段值时请使用 {@link #insertInto(Object)}。</p>
     */
    public InsertQuery insertInto(Class<?> modelClass) {
        return insertInto(Table.of(modelClass));
    }

    /**
     * 根据模型实例自动推导 insert 表，并读取 @Column 字段生成 values。
     *
     * <pre>{@code
     * RenderedSql sql = ctx.insertInto(userModel).render();
     * }</pre>
     */
    public InsertQuery insertInto(Object model) {
        return insertInto(model, ModelSetOptions.defaults());
    }

    /**
     * 根据模型实例自动推导 insert 表，并跳过 null 字段。
     */
    public InsertQuery insertIntoNonNull(Object model) {
        return insertInto(model, ModelSetOptions.ignoreNullValues());
    }

    /**
     * 根据模型实例自动推导 insert 表，并用 options 控制 null / primaryKey 处理。
     */
    public InsertQuery insertInto(Object model, ModelSetOptions options) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(options, "options");
        var meta = ModelResolver.resolve(model);
        return insertInto(Table.of(meta.table())).set(model, options);
    }

    public UpdateQuery update(Table table) {
        return new UpdateQuery(dialect, settings, table);
    }

    /**
     * 根据模型 Class 上的 @Table 注解推导 update 表。
     *
     * <p>只推导表，不会自动 set 字段值。需要字段值时请使用 {@link #update(Object)}。</p>
     */
    public UpdateQuery update(Class<?> modelClass) {
        return update(Table.of(modelClass));
    }

    /**
     * 根据模型实例自动推导 update 表，并读取 @Column 字段生成 set。
     *
     * <p>默认不更新 primaryKey 字段，避免把主键放进 set 子句。where 条件仍需要手动指定。</p>
     */
    public UpdateQuery update(Object model) {
        return update(model, ModelSetOptions.excludePrimaryKeys());
    }

    /**
     * 根据模型实例自动推导 update 表，并读取非 null 的非主键字段生成 set。
     */
    public UpdateQuery updateNonNull(Object model) {
        return update(model, ModelSetOptions.excludePrimaryKeys().withIgnoreNulls(true));
    }

    /**
     * 根据模型实例自动推导 update 表，并用 options 控制 null / primaryKey 处理。
     */
    public UpdateQuery update(Object model, ModelSetOptions options) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(options, "options");
        var meta = ModelResolver.resolve(model);
        return update(Table.of(meta.table())).set(model, options);
    }

    /**
     * 直接创建 update 并批量 set 字段值。
     */
    public UpdateQuery update(Table table, Map<? extends Field<?>, ?> values) {
        return update(table).set(values);
    }

    /**
     * 直接创建 update 并批量 set Value 类型字段值。
     */
    public UpdateQuery updateValues(Table table, Map<? extends Field<?>, ? extends Value<?>> values) {
        return update(table).setValues(values);
    }

    /**
     * 直接创建 update 并从模型对象批量 set。
     */
    public UpdateQuery update(Table table, Object model) {
        return update(table).set(model);
    }

    /**
     * 直接创建 update 并从模型对象批量 set，支持 options 控制 null / primaryKey。
     */
    public UpdateQuery update(Table table, Object model, ModelSetOptions options) {
        return update(table).set(model, options);
    }

    public DeleteQuery deleteFrom(Table table) {
        return new DeleteQuery(dialect, settings, table);
    }

    public List<Field<?>> fields(Field<?>... fields) {
        return Arrays.asList(fields);
    }

    public Condition noCondition() {
        return DSL.noCondition();
    }
}
