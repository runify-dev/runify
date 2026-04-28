package com.run.sql.dialect.renderer;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

/** Optional JSON expression renderer. Built-ins support common text extraction only. */
public interface JsonRenderer {
    String text(RenderContext ctx, Field<?> jsonField, String path);
}
