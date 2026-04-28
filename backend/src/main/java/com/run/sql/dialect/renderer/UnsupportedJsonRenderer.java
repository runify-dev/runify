package com.run.sql.dialect.renderer;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public class UnsupportedJsonRenderer implements JsonRenderer {
    @Override
    public String text(RenderContext ctx, Field<?> jsonField, String path) {
        throw new UnsupportedOperationException("Dialect " + ctx.dialect().dialectName() + " does not support jsonText() by default");
    }
}
