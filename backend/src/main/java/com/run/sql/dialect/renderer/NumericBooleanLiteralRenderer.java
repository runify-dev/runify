package com.run.sql.dialect.renderer;

/** Renders boolean values as 1/0 for databases that don't have a native SQL boolean type. */
public class NumericBooleanLiteralRenderer extends StandardLiteralRenderer {
    @Override
    protected String booleanLiteral(boolean value) {
        return value ? "1" : "0";
    }
}
