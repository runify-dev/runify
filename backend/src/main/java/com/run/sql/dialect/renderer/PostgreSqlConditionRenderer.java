package com.run.sql.dialect.renderer;

import com.run.sql.Value;
import com.run.sql.model.Field;
import com.run.sql.render.RenderContext;

/** PostgreSQL/Kingbase regex syntax: ~, ~*, !~, !~*. */
public class PostgreSqlConditionRenderer extends StandardConditionRenderer {
    @Override
    public String regex(RenderContext ctx, Field<?> field, Value<?> pattern, boolean caseSensitive, boolean not) {
        String operator;
        if (caseSensitive) {
            operator = not ? " !~ " : " ~ ";
        } else {
            operator = not ? " !~* " : " ~* ";
        }
        return field.renderRef(ctx) + operator + pattern.render(ctx);
    }
}
