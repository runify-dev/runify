package com.run.sql.query;

import com.run.sql.condition.Condition;
import com.run.sql.dialect.Dialect;
import com.run.sql.model.Table;
import com.run.sql.render.RenderContext;
import com.run.sql.render.RenderSettings;

public final class DeleteQuery extends AbstractQuery {
    private final Table table;
    private Condition where;

    public DeleteQuery(Dialect dialect, RenderSettings settings, Table table) {
        super(dialect, settings);
        this.table = table;
    }

    public DeleteQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    public DeleteQuery and(Condition condition) {
        this.where = this.where == null ? condition : this.where.and(condition);
        return this;
    }

    public DeleteQuery or(Condition condition) {
        this.where = this.where == null ? condition : this.where.or(condition);
        return this;
    }

    @Override
    protected String renderSql(RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(table.render(ctx));

        if (where != null && !where.isEmpty()) {
            sb.append(" where ").append(where.render(ctx));
        }

        return sb.toString();
    }
}
