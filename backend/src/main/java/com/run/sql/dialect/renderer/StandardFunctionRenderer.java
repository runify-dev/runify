package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.render.RenderContext;

import java.util.List;
import java.util.stream.Collectors;

/** Default SQL function renderer. */
public class StandardFunctionRenderer implements FunctionRenderer {
    @Override
    public String concat(RenderContext ctx, List<? extends Value<?>> values) {
        if (values == null || values.isEmpty()) {
            return "''";
        }
        return values.stream().map(v -> v.render(ctx)).collect(Collectors.joining(" || "));
    }

    @Override
    public String currentTimestamp(RenderContext ctx) {
        return "current_timestamp";
    }
}
