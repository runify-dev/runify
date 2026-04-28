package com.run.sql.model;

import com.run.sql.Value;
import com.run.sql.render.RenderContext;

public final class Val<T> implements Value<T> {
    private final T value;

    private Val(T value) {
        this.value = value;
    }

    public static <T> Val<T> of(T value) {
        return new Val<>(value);
    }

    @Override
    public String render(RenderContext ctx) {
        return ctx.bind(null, value);
    }
}
