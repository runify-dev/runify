package com.run.sql.condition;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

import java.util.Collection;
import java.util.List;

public final class InListCondition implements Condition {
    private final Field<?> field;
    private final Collection<?> values;
    private final String baseName;
    private final boolean not;

    public InListCondition(Field<?> field, Collection<?> values, String baseName, boolean not) {
        this.field = field;
        this.values = values == null ? List.of() : values;
        this.baseName = baseName;
        this.not = not;
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().conditions().inList(ctx, field, values, baseName, not);
    }
}
