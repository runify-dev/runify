package com.run.sql.condition;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public final class CompareCondition implements Condition {
    private final Field<?> left;
    private final String operator;
    private final Value<?> right;

    public CompareCondition(Field<?> left, String operator, Value<?> right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().compare(ctx, left, operator, right);
    }
}
