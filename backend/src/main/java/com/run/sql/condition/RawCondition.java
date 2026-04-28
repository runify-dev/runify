package com.run.sql.condition;

import com.run.sql.render.RenderContext;

import java.util.Map;
import java.util.Objects;

public final class RawCondition implements Condition {
    private final String sqlTemplate;
    private final Map<String, Object> params;

    public RawCondition(String sqlTemplate, Map<String, Object> params) {
        this.sqlTemplate = Objects.requireNonNull(sqlTemplate, "sqlTemplate");
        this.params = params == null ? Map.of() : params;
    }

    @Override
    public String render(RenderContext ctx) {
        params.forEach(ctx::putNamedParam);
        return sqlTemplate;
    }
}
