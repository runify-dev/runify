package com.run.sql.model;

import com.run.sql.Value;
import com.run.sql.render.RenderContext;

public final class Inline<T> implements Value<T> {
    private final T value;

    private Inline(T value) {
        this.value = value;
    }

    public static <T> Inline<T> of(T value) {
        return new Inline<>(value);
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.dialect().literals().render(ctx, value);
    }
}
