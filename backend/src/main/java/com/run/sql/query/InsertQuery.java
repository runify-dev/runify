package com.run.sql.query;

import com.run.sql.Value;
import com.run.sql.dialect.Dialect;
import com.run.sql.metadata.ModelColumnMeta;
import com.run.sql.metadata.ModelResolver;
import com.run.sql.model.Field;
import com.run.sql.model.ModelSetOptions;
import com.run.sql.model.Table;
import com.run.sql.model.Val;
import com.run.sql.render.RenderContext;
import com.run.sql.render.RenderSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.run.sql.DSL.field;
import static com.run.sql.DSL.param;

public final class InsertQuery extends AbstractQuery {
    private final Table table;
    private final LinkedHashMap<Field<?>, Value<?>> values = new LinkedHashMap<>();

    public InsertQuery(Dialect dialect, RenderSettings settings, Table table) {
        super(dialect, settings);
        this.table = table;
    }

    public InsertQuery set(Field<?> field, Object value) {
        values.put(Objects.requireNonNull(field, "field"), value instanceof Value<?> v ? v : Val.of(value));
        return this;
    }

    /**
     * 批量设置字段值。
     *
     * <p>value 可以是普通对象，也可以是 {@link Value}，例如 {@code Param}、{@code Val}、{@code Inline}、表达式字段等。</p>
     */
    public InsertQuery set(Map<? extends Field<?>, ?> fieldValues) {
        Objects.requireNonNull(fieldValues, "fieldValues");
        for (Map.Entry<? extends Field<?>, ?> entry : fieldValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set。
     *
     * <p>默认包含 null 值，也包含 primaryKey 字段。适合 insert 场景。</p>
     */
    public InsertQuery set(Object model) {
        return set(model, ModelSetOptions.defaults());
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set，并跳过 null 值。
     */
    public InsertQuery setNonNull(Object model) {
        return set(model, ModelSetOptions.ignoreNullValues());
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set，并允许控制是否跳过 null / primaryKey。
     */
    public InsertQuery set(Object model, ModelSetOptions options) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(options, "options");

        for (ModelColumnMeta column : ModelResolver.resolve(model).columns()) {
            if (!options.includePrimaryKeys() && column.primaryKey()) {
                continue;
            }

            Object value = column.read(model);
            if (options.ignoreNulls() && value == null) {
                continue;
            }

            set(field(column.columnName()), value);
        }

        return this;
    }

    public InsertQuery setValue(Field<?> field, Value<?> value) {
        values.put(Objects.requireNonNull(field, "field"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * 批量设置 Value 类型字段值。
     *
     * <p>这个方法主要用于你希望编译期限制 value 必须是 {@link Value} 的场景，
     * 例如 {@code Map<Field<?>, Param<?>>}。</p>
     */
    public InsertQuery setValues(Map<? extends Field<?>, ? extends Value<?>> fieldValues) {
        Objects.requireNonNull(fieldValues, "fieldValues");
        for (Map.Entry<? extends Field<?>, ? extends Value<?>> entry : fieldValues.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public InsertQuery set(String fieldName, Object value) {
        return set(field(fieldName), value);
    }

    public InsertQuery setParam(String fieldName, String paramName, Object value) {
        return setValue(field(fieldName), param(paramName, value));
    }

    @Override
    protected String renderSql(RenderContext ctx) {
        if (values.isEmpty()) {
            throw new IllegalStateException("insert values is empty");
        }

        List<String> columns = new ArrayList<>();
        List<String> holders = new ArrayList<>();

        for (Map.Entry<Field<?>, Value<?>> entry : values.entrySet()) {
            columns.add(entry.getKey().renderRef(ctx));
            holders.add(entry.getValue().render(ctx));
        }

        return "insert into "
                + table.render(ctx)
                + " ("
                + String.join(", ", columns)
                + ") values ("
                + String.join(", ", holders)
                + ")";
    }
}
