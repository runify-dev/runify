package com.run.sql.condition;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public final class BetweenCondition implements Condition {
    private final Field<?> field;
    private final Value<?> start;
    private final Value<?> end;
    private final boolean not;

    public BetweenCondition(Field<?> field, Value<?> start, Value<?> end, boolean not) {
        this.field = field;
        this.start = start;
        this.end = end;
        this.not = not;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().between(ctx, field, start, end, not);
    }
}
