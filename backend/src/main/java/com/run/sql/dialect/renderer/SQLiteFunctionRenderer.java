package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** SQLite-specific functions. */
public class SQLiteFunctionRenderer extends StandardFunctionRenderer {
    @Override
    public String currentTimestamp(RenderContext ctx) {
        return "datetime('now')";
    }
}
