package com.run.sql.condition;

import com.run.sql.model.Field;
import com.run.sql.query.SelectQuery;
import com.run.sql.render.RenderContext;

public final class InSubQueryCondition implements Condition {
    private final Field<?> field;
    private final SelectQuery subQuery;
    private final boolean not;

    public InSubQueryCondition(Field<?> field, SelectQuery subQuery, boolean not) {
        this.field = field;
        this.subQuery = subQuery;
        this.not = not;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().inSubQuery(ctx, field, subQuery, not);
    }
}
