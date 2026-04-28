package com.run.sql.condition;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public final class RegexCondition implements Condition {
    private final Field<?> field;
    private final Value<?> pattern;
    private final boolean caseSensitive;
    private final boolean not;

    public RegexCondition(Field<?> field, Value<?> pattern, boolean caseSensitive, boolean not) {
        this.field = field;
        this.pattern = pattern;
        this.caseSensitive = caseSensitive;
        this.not = not;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().regex(ctx, field, pattern, caseSensitive, not);
    }
}
