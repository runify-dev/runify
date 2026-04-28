package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Default inline literal renderer. */
public class StandardLiteralRenderer implements LiteralRenderer {
    @Override
    public String render(RenderContext ctx, Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean b) {
            return booleanLiteral(b);
        }
        return stringLiteral(value.toString());
    }

    protected String booleanLiteral(boolean value) {
        return value ? "true" : "false";
    }

    protected String stringLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
