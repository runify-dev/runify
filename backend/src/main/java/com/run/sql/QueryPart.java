package com.run.sql;

import com.run.sql.render.RenderContext;

public interface QueryPart {
    String render(RenderContext ctx);
}
