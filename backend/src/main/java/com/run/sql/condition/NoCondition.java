package com.run.sql.condition;

import com.run.sql.render.RenderContext;

public enum NoCondition implements Condition {
    INSTANCE;

    @Override
    public String render(RenderContext ctx) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
