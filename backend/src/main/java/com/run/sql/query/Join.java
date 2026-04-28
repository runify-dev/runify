package com.run.sql.query;

import com.run.sql.QueryPart;
import com.run.sql.condition.Condition;
import com.run.sql.model.Table;
import com.run.sql.render.RenderContext;

final class Join implements QueryPart {
    private final String type;
    private final Table table;
    private final Condition on;

    Join(String type, Table table, Condition on) {
        this.type = type;
        this.table = table;
        this.on = on;
    }

    @Override
    public String render(RenderContext ctx) {
        String sql = type + " " + table.render(ctx);
        if (on != null && !on.isEmpty()) {
            sql += " on " + on.render(ctx);
        }
        return sql;
    }
}
