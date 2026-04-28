package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

/** MySQL 8+ regex syntax using REGEXP_LIKE with explicit case flag. */
public class MySqlConditionRenderer extends StandardConditionRenderer {
    @Override
    public String regex(RenderContext ctx, Field<?> field, Value<?> pattern, boolean caseSensitive, boolean not) {
        String flag = caseSensitive ? "c" : "i";
        String sql = "regexp_like(" + field.renderRef(ctx) + ", " + pattern.render(ctx) + ", '" + flag + "')";
        return not ? "not " + sql : sql;
    }
}
