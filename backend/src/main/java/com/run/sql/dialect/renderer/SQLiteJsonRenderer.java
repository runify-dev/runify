package com.run.sql.dialect.renderer;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

public class SQLiteJsonRenderer implements JsonRenderer {
    @Override
    public String text(RenderContext ctx, Field<?> jsonField, String path) {
        return "json_extract(" + jsonField.renderRef(ctx) + ", " + ctx.dialect().literals().render(ctx, path) + ")";
    }
}
