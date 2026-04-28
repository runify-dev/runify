package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Renders inline literals. Prefer bind values for user input; inline is for trusted constants. */
public interface LiteralRenderer {
    String render(RenderContext ctx, Object value);
}
