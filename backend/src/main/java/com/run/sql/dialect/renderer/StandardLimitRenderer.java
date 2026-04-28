package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** PostgreSQL / SQLite / H2 style: limit n offset m. */
public class StandardLimitRenderer implements LimitRenderer {
    @Override
    public String render(RenderContext ctx, String sql, Long limit, Long offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql);
        if (limit != null) {
            sb.append(" limit ").append(ctx.bind("_limit", limit));
        }
        if (offset != null) {
            sb.append(" offset ").append(ctx.bind("_offset", offset));
        }
        return sb.toString();
    }
}
