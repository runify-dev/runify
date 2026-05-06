package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Oracle 12c+ / DM style: offset ... rows fetch next ... rows only. */
public class OracleLimitRenderer implements LimitRenderer {
    @Override
    public String render(RenderContext ctx, String sql, Long limit, Long offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql);
        if (offset != null) {
            sb.append(" offset ").append(ctx.bind("offset", offset)).append(" rows");
        }
        if (limit != null) {
            if (offset == null) {
                sb.append(" fetch first ").append(ctx.bind("limit", limit)).append(" rows only");
            } else {
                sb.append(" fetch next ").append(ctx.bind("limit", limit)).append(" rows only");
            }
        }
        return sb.toString();
    }
}
