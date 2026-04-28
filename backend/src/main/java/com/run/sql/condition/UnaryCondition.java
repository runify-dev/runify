package com.run.sql.condition;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public final class UnaryCondition implements Condition {
    private final Field<?> field;
    private final String operator;

    public UnaryCondition(Field<?> field, String operator) {
        this.field = field;
        this.operator = operator;
    }

    @Override
    public String render(RenderContext ctx) {
        if ("is null".equalsIgnoreCase(operator)) {
            return ctx.dialect().conditions().isNull(ctx, field, false);
        }
        if ("is not null".equalsIgnoreCase(operator)) {
            return ctx.dialect().conditions().isNull(ctx, field, true);
        }
        return field.renderRef(ctx) + " " + operator;
    }
}
