package com.run.sql.query;

import com.run.sql.Query;
import com.run.sql.RenderedSql;
import com.run.sql.dialect.Dialect;
import com.run.sql.render.RenderContext;
import com.run.sql.render.RenderSettings;

public abstract class AbstractQuery implements Query {
    protected final Dialect dialect;
    protected final RenderSettings settings;

    protected AbstractQuery(Dialect dialect, RenderSettings settings) {
        this.dialect = dialect;
        this.settings = settings;
    }

    @Override
    public RenderedSql render() {
        RenderContext ctx = new RenderContext(dialect, settings);
        return new RenderedSql(renderSql(ctx), ctx.params());
    }

    protected abstract String renderSql(RenderContext ctx);
}
