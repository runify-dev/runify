package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Oracle/DM-specific functions. */
public class OracleFunctionRenderer extends StandardFunctionRenderer {
    @Override
    public String currentTimestamp(RenderContext ctx) {
        return "systimestamp";
    }
}
