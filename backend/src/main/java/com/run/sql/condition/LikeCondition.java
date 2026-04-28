package com.run.sql.condition;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public final class LikeCondition implements Condition {
    private final Field<?> field;
    private final Value<?> pattern;
    private final boolean not;

    public LikeCondition(Field<?> field, Value<?> pattern, boolean not) {
        this.field = field;
        this.pattern = pattern;
        this.not = not;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().like(ctx, field, pattern, not);
    }
}
