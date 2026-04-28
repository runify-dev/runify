package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.query.SelectQuery;
import com.run.sql.render.RenderContext;

import java.util.Collection;

/** Renders condition/operator syntax that differs by database. */
public interface ConditionRenderer {
    String compare(RenderContext ctx, Field<?> left, String operator, Value<?> right);

    String like(RenderContext ctx, Field<?> field, Value<?> pattern, boolean not);

    String regex(RenderContext ctx, Field<?> field, Value<?> pattern, boolean caseSensitive, boolean not);

    String isNull(RenderContext ctx, Field<?> field, boolean not);

    String between(RenderContext ctx, Field<?> field, Value<?> start, Value<?> end, boolean not);

    String inList(RenderContext ctx, Field<?> field, Collection<?> values, String baseName, boolean not);

    String inSubQuery(RenderContext ctx, Field<?> field, SelectQuery subQuery, boolean not);
}
