package com.run.sql.dialect.renderer;

import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

/** PostgreSQL JSON text extraction. Simple $.name paths use ->>, otherwise jsonb_path_query_first. */
public class PostgreSqlJsonRenderer implements JsonRenderer {
    @Override
    public String text(RenderContext ctx, Field<?> jsonField, String path) {
        if (path != null && path.startsWith("$.") && path.indexOf('.', 2) < 0 && path.indexOf('[', 2) < 0) {
            String key = path.substring(2);
            return jsonField.renderRef(ctx) + " ->> " + ctx.dialect().literals().render(ctx, key);
        }
        return "jsonb_path_query_first(" + jsonField.renderRef(ctx) + "::jsonb, " + ctx.dialect().literals().render(ctx, path) + ") #>> '{}'";
    }
}
