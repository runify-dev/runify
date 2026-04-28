package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Renders database-specific pagination syntax. */
public interface LimitRenderer {
    String render(RenderContext ctx, String sql, Long limit, Long offset);
}
