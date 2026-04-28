package com.run.sql.model;

import com.run.sql.Value;
import com.run.sql.condition.*;
import com.run.sql.lambda.LambdaColumnResolver;
import com.run.sql.lambda.SerializableFunction;
import com.run.sql.query.SelectQuery;
import com.run.sql.render.RenderContext;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public final class Field<T> implements Value<T> {
    private final String catalog;
    private final String schema;
    private final String table;
    private final String nameOrExpression;
    private final boolean raw;
    private final String alias;
    private final Function<RenderContext, String> renderer;

    private Field(String catalog, String schema, String table, String nameOrExpression, boolean raw, String alias,
                  Function<RenderContext, String> renderer) {
        this.catalog = catalog;
        this.schema = schema;
        this.table = table;
        this.nameOrExpression = Objects.requireNonNull(nameOrExpression, "nameOrExpression");
        this.raw = raw;
        this.alias = alias;
        this.renderer = renderer;
    }

    public static <T> Field<T> of(String name) {
        return new Field<>(null, null, null, name, false, null, null);
    }

    public static <T, R> Field<R> of(SerializableFunction<T, R> getter) {
        return new Field<>(null, null, null, LambdaColumnResolver.resolve(getter), false, null, null);
    }

    public static <T> Field<T> qualified(String catalog, String schema, String table, String name) {
        return new Field<>(catalog, schema, table, name, false, null, null);
    }

    public static <T, R> Field<R> qualified(String catalog, String schema, String table, SerializableFunction<T, R> getter) {
        return new Field<>(catalog, schema, table, LambdaColumnResolver.resolve(getter), false, null, null);
    }

    public static <T> Field<T> raw(String expression) {
        return new Field<>(null, null, null, expression, true, null, null);
    }

    public static <T> Field<T> expression(String debugName, Function<RenderContext, String> renderer) {
        return new Field<>(null, null, null, debugName, false, null, Objects.requireNonNull(renderer, "renderer"));
    }

    public Field<T> as(String alias) {
        return new Field<>(catalog, schema, table, nameOrExpression, raw, alias, renderer);
    }

    public String nameOrExpression() {
        return nameOrExpression;
    }

    public String renderRef(RenderContext ctx) {
        if (renderer != null) {
            return renderer.apply(ctx);
        }
        if (raw) {
            return nameOrExpression;
        }
        return ctx.fieldIdentifier(catalog, schema, table, nameOrExpression);
    }

    @Override
    public String render(RenderContext ctx) {
        String sql = renderRef(ctx);
        if (alias == null || alias.isBlank()) {
            return sql;
        }
        return sql + " " + ctx.identifier(alias);
    }

    public Condition eq(Value<?> value) {
        if (value == null) {
            return isNull();
        }
        return new CompareCondition(this, "=", value);
    }

    public Condition eq(Object value) {
        if (value == null) {
            return isNull();
        }
        if (value instanceof Value<?> v) {
            return eq(v);
        }
        return eq(Val.of(value));
    }

    public Condition ne(Value<?> value) {
        if (value == null) {
            return isNotNull();
        }
        return new CompareCondition(this, "<>", value);
    }

    public Condition ne(Object value) {
        if (value == null) {
            return isNotNull();
        }
        if (value instanceof Value<?> v) {
            return ne(v);
        }
        return ne(Val.of(value));
    }

    public Condition notEqual(Value<?> value) {
        return ne(value);
    }

    public Condition notEqual(Object value) {
        return ne(value);
    }

    public Condition gt(Object value) {
        return new CompareCondition(this, ">", value instanceof Value<?> v ? v : Val.of(value));
    }

    public Condition ge(Object value) {
        return new CompareCondition(this, ">=", value instanceof Value<?> v ? v : Val.of(value));
    }

    public Condition lt(Object value) {
        return new CompareCondition(this, "<", value instanceof Value<?> v ? v : Val.of(value));
    }

    public Condition le(Object value) {
        return new CompareCondition(this, "<=", value instanceof Value<?> v ? v : Val.of(value));
    }

    public Condition like(Object value) {
        return new LikeCondition(this, value instanceof Value<?> v ? v : Val.of(value), false);
    }

    public Condition notLike(Object value) {
        return new LikeCondition(this, value instanceof Value<?> v ? v : Val.of(value), true);
    }

    public Condition regex(Object pattern) {
        return regex(pattern, true);
    }

    public Condition regex(Object pattern, boolean caseSensitive) {
        return new RegexCondition(this, pattern instanceof Value<?> v ? v : Val.of(pattern), caseSensitive, false);
    }

    public Condition notRegex(Object pattern) {
        return notRegex(pattern, true);
    }

    public Condition notRegex(Object pattern, boolean caseSensitive) {
        return new RegexCondition(this, pattern instanceof Value<?> v ? v : Val.of(pattern), caseSensitive, true);
    }

    public Condition isNull() {
        return new UnaryCondition(this, "is null");
    }

    public Condition isNotNull() {
        return new UnaryCondition(this, "is not null");
    }

    public Condition in(Collection<?> values) {
        return new InListCondition(this, values, null, false);
    }

    public Condition inParam(String baseName, Collection<?> values) {
        return new InListCondition(this, values, baseName, false);
    }

    public Condition notIn(Collection<?> values) {
        return new InListCondition(this, values, null, true);
    }

    public Condition notInParam(String baseName, Collection<?> values) {
        return new InListCondition(this, values, baseName, true);
    }

    public Condition in(SelectQuery subQuery) {
        return new InSubQueryCondition(this, subQuery, false);
    }

    public Condition notIn(SelectQuery subQuery) {
        return new InSubQueryCondition(this, subQuery, true);
    }

    public Condition between(Object start, Object end) {
        return new BetweenCondition(this,
                start instanceof Value<?> v ? v : Val.of(start),
                end instanceof Value<?> v ? v : Val.of(end),
                false);
    }

    public Condition notBetween(Object start, Object end) {
        return new BetweenCondition(this,
                start instanceof Value<?> v ? v : Val.of(start),
                end instanceof Value<?> v ? v : Val.of(end),
                true);
    }

    public SortField asc() {
        return new SortField(this, "asc");
    }

    public SortField desc() {
        return new SortField(this, "desc");
    }
}
