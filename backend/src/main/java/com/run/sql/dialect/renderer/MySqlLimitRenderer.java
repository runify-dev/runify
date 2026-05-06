package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** MySQL style: limit offset, row_count. */
public class MySqlLimitRenderer implements LimitRenderer {
    @Override
    public String render(RenderContext ctx, String sql, Long limit, Long offset) {
        if (limit == null && offset == null) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql);
        if (limit != null && offset != null) {
            sb.append(" limit ")
                    .append(ctx.bind("offset", offset))
                    .append(", ")
                    .append(ctx.bind("limit", limit));
        } else if (limit != null) {
            sb.append(" limit ").append(ctx.bind("limit", limit));
        } else {
            sb.append(" limit ").append(ctx.bind("offset", offset)).append(", 18446744073709551615");
        }
        return sb.toString();
    }
}
