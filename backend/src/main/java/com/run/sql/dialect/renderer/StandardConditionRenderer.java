package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.model.Param;
import com.run.sql.query.SelectQuery;
import com.run.sql.render.RenderContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Default SQL condition renderer. */
public class StandardConditionRenderer implements ConditionRenderer {

    @Override
    public String compare(RenderContext ctx, Field<?> left, String operator, Value<?> right) {
        String rightSql = right instanceof Field<?> field ? field.renderRef(ctx) : right.render(ctx);
        return left.renderRef(ctx) + " " + operator + " " + rightSql;
    }

    @Override
    public String like(RenderContext ctx, Field<?> field, Value<?> pattern, boolean not) {
        return field.renderRef(ctx) + (not ? " not like " : " like ") + pattern.render(ctx);
    }

    @Override
    public String regex(RenderContext ctx, Field<?> field, Value<?> pattern, boolean caseSensitive, boolean not) {
        return field.renderRef(ctx) + (not ? " not regexp " : " regexp ") + pattern.render(ctx);
    }

    @Override
    public String isNull(RenderContext ctx, Field<?> field, boolean not) {
        return field.renderRef(ctx) + (not ? " is not null" : " is null");
    }

    @Override
    public String between(RenderContext ctx, Field<?> field, Value<?> start, Value<?> end, boolean not) {
        return field.renderRef(ctx)
                + (not ? " not between " : " between ")
                + start.render(ctx)
                + " and "
                + end.render(ctx);
    }

    @Override
    public String inList(RenderContext ctx, Field<?> field, Collection<?> values, String baseName, boolean not) {
        if (values == null || values.isEmpty()) {
            return not ? "1 = 1" : "1 = 0";
        }

        List<?> list = new ArrayList<>(values);
        int max = ctx.dialect().maxInListSize();

        if (max > 0 && list.size() > max) {
            List<String> chunks = new ArrayList<>();
            for (int i = 0; i < list.size(); i += max) {
                List<?> sub = list.subList(i, Math.min(i + max, list.size()));
                chunks.add(renderInChunk(ctx, field, sub, baseName, not, i));
            }
            return "(" + String.join(not ? " and " : " or ", chunks) + ")";
        }

        return renderInChunk(ctx, field, list, baseName, not, 0);
    }

    @Override
    public String inSubQuery(RenderContext ctx, Field<?> field, SelectQuery subQuery, boolean not) {
        return field.renderRef(ctx) + (not ? " not in " : " in ") + subQuery.render(ctx);
    }

    protected String renderInChunk(RenderContext ctx, Field<?> field, List<?> list, String baseName, boolean not, int offset) {
        List<String> holders = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Param<?> p && p.isNamedOnly()) {
                ctx.registerParamName(p.name());
                holders.add(ctx.paramTemplate(p.name()));
            } else {
                String name;
                if (item instanceof Param<?> p && p.hasName()) {
                    name = p.name();
                } else {
                    name = baseName == null ? null : baseName + "_" + (offset + i);
                }
                holders.add(ctx.bind(name, item));
            }
        }
        return field.renderRef(ctx)
                + (not ? " not in " : " in ")
                + "(" + String.join(", ", holders) + ")";
    }
}
