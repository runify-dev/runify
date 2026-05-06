package com.run.sql.query;

import com.run.sql.Value;
import com.run.sql.condition.Condition;
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

public final class UpdateQuery extends AbstractQuery {
    private final Table table;
    private final LinkedHashMap<Field<?>, Value<?>> values = new LinkedHashMap<>();
    private Condition where;

    public UpdateQuery(Dialect dialect, RenderSettings settings, Table table) {
        super(dialect, settings);
        this.table = table;
    }

    public UpdateQuery set(Field<?> field, Object value) {
        values.put(Objects.requireNonNull(field, "field"), value instanceof Value<?> v ? v : Val.of(value));
        return this;
    }

    /**
     * 批量设置字段值。value 可以是普通对象，也可以是 {@link Value}，例如 {@code Param}。
     */
    public UpdateQuery set(Map<? extends Field<?>, ?> fieldValues) {
        Objects.requireNonNull(fieldValues, "fieldValues");
        for (Map.Entry<? extends Field<?>, ?> entry : fieldValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set。
     *
     * <p>默认包含 null 值，也包含 primaryKey 字段。update 场景如果不希望更新主键，可以使用
     * {@code set(model, ModelSetOptions.excludePrimaryKeys())}。</p>
     */
    public UpdateQuery set(Object model) {
        return set(model, ModelSetOptions.defaults());
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set，并跳过 null 值。
     */
    public UpdateQuery setNonNull(Object model) {
        return set(model, ModelSetOptions.ignoreNullValues());
    }

    /**
     * 根据模型对象上的 @Column 注解批量 set，并允许控制是否跳过 null / primaryKey。
     */
    public UpdateQuery set(Object model, ModelSetOptions options) {
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

            // 模型对象 set 时，参数名使用 Column.name() 对应的列名，避免生成 #{p1} / #{p2}。
            // 如果同一个列名在同一条 SQL 中被绑定了不同值，RenderContext 会自动追加 _1、_2 后缀。
            setValue(field(column.columnName()), param(column.columnName(), value));
        }

        return this;
    }

    public UpdateQuery setValue(Field<?> field, Value<?> value) {
        values.put(Objects.requireNonNull(field, "field"), Objects.requireNonNull(value, "value"));
        return this;
    }

    public UpdateQuery setValues(Map<? extends Field<?>, ? extends Value<?>> fieldValues) {
        Objects.requireNonNull(fieldValues, "fieldValues");
        for (Map.Entry<? extends Field<?>, ? extends Value<?>> entry : fieldValues.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public UpdateQuery set(String fieldName, Object value) {
        return set(field(fieldName), value);
    }

    public UpdateQuery setParam(String fieldName, String paramName, Object value) {
        return setValue(field(fieldName), param(paramName, value));
    }

    public UpdateQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    public UpdateQuery and(Condition condition) {
        this.where = this.where == null ? condition : this.where.and(condition);
        return this;
    }

    public UpdateQuery or(Condition condition) {
        this.where = this.where == null ? condition : this.where.or(condition);
        return this;
    }

    @Override
    protected String renderSql(RenderContext ctx) {
        if (values.isEmpty()) {
            throw new IllegalStateException("update set values is empty");
        }

        List<String> sets = new ArrayList<>();
        for (Map.Entry<Field<?>, Value<?>> entry : values.entrySet()) {
            sets.add(entry.getKey().renderRef(ctx) + " = " + entry.getValue().render(ctx));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("update ")
                .append(table.render(ctx))
                .append(" set ")
                .append(String.join(", ", sets));

        if (where != null && !where.isEmpty()) {
            sb.append(" where ").append(where.render(ctx));
        }

        return sb.toString();
    }
}
