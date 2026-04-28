package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.render.RenderContext;

import java.util.List;

/** Renders database-specific scalar functions. */
public interface FunctionRenderer {
    String concat(RenderContext ctx, List<? extends Value<?>> values);

    String currentTimestamp(RenderContext ctx);
}
