package com.run.sql.condition;

import com.run.sql.render.RenderContext;

public final class LogicalCondition implements Condition {
    private final Condition left;
    private final String operator;
    private final Condition right;

    public LogicalCondition(Condition left, String operator, Condition right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String render(RenderContext ctx) {
        return "(" + left.render(ctx) + " " + operator + " " + right.render(ctx) + ")";
    }
}
