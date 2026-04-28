package com.run.sql.model;

import com.run.sql.QueryPart;
import com.run.sql.render.RenderContext;

public final class SortField implements QueryPart {
    private final Field<?> field;
    private final String direction;

    public SortField(Field<?> field, String direction) {
        this.field = field;
        this.direction = direction;
    }

    @Override
    public String render(RenderContext ctx) {
        return field.renderRef(ctx) + " " + direction;
    }
}
