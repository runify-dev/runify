package com.run.sql.query;

import com.run.sql.QueryPart;
import com.run.sql.Value;
import com.run.sql.condition.Condition;
import com.run.sql.dialect.Dialect;
import com.run.sql.model.Field;
import com.run.sql.model.SortField;
import com.run.sql.model.Table;
import com.run.sql.render.RenderContext;
import com.run.sql.render.RenderSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.run.sql.DSL.asterisk;

public final class SelectQuery extends AbstractQuery implements Value<Object> {
    private final List<Field<?>> fields;
    private Table from;
    private final List<Join> joins = new ArrayList<>();
    private Condition where;
    private final List<Field<?>> groupBy = new ArrayList<>();
    private Condition having;
    private final List<SortField> orderBy = new ArrayList<>();
    private Long limit;
    private Long offset;

    public SelectQuery(Dialect dialect, RenderSettings settings, List<Field<?>> fields) {
        super(dialect, settings);
        this.fields = fields == null || fields.isEmpty() ? List.of(asterisk()) : new ArrayList<>(fields);
    }

    public SelectQuery from(Table table) {
        this.from = table;
        return this;
    }

    public SelectQuery join(Table table, Condition on) {
        joins.add(new Join("join", table, on));
        return this;
    }

    public SelectQuery leftJoin(Table table, Condition on) {
        joins.add(new Join("left join", table, on));
        return this;
    }

    public SelectQuery rightJoin(Table table, Condition on) {
        joins.add(new Join("right join", table, on));
        return this;
    }

    public SelectQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    public SelectQuery and(Condition condition) {
        this.where = this.where == null ? condition : this.where.and(condition);
        return this;
    }

    public SelectQuery or(Condition condition) {
        this.where = this.where == null ? condition : this.where.or(condition);
        return this;
    }

    public SelectQuery groupBy(Field<?>... fields) {
        groupBy.addAll(Arrays.asList(fields));
        return this;
    }

    public SelectQuery having(Condition condition) {
        this.having = condition;
        return this;
    }

    public SelectQuery orderBy(SortField... fields) {
        orderBy.addAll(Arrays.asList(fields));
        return this;
    }

    public SelectQuery orderBy(Collection<? extends SortField> fields) {
        orderBy.addAll(fields);
        return this;
    }

    public SelectQuery limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public SelectQuery offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public SelectQuery limit(long limit) {
        this.limit = limit;
        return this;
    }

    public SelectQuery offset(long offset) {
        this.offset = offset;
        return this;
    }

    public SelectQuery page(long pageNo, long pageSize) {
        long safePageNo = Math.max(pageNo, 1L);
        long safePageSize = Math.max(pageSize, 1L);
        this.limit = safePageSize;
        this.offset = (safePageNo - 1L) * safePageSize;
        return this;
    }

    @Override
    protected String renderSql(RenderContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(joinParts(ctx, fields));

        if (from != null) {
            sb.append(" from ").append(from.render(ctx));
        }

        for (Join join : joins) {
            sb.append(" ").append(join.render(ctx));
        }

        if (where != null && !where.isEmpty()) {
            sb.append(" where ").append(where.render(ctx));
        }

        if (!groupBy.isEmpty()) {
            sb.append(" group by ").append(joinFieldRefs(ctx, groupBy));
        }

        if (having != null && !having.isEmpty()) {
            sb.append(" having ").append(having.render(ctx));
        }

        if (!orderBy.isEmpty()) {
            sb.append(" order by ").append(joinParts(ctx, orderBy));
        }

        return dialect.renderLimitOffset(sb.toString(), ctx, limit, offset);
    }

    @Override
    public String render(RenderContext ctx) {
        return "(" + renderSql(ctx) + ")";
    }

    private static String joinParts(RenderContext ctx, Collection<? extends QueryPart> parts) {
        List<String> result = new ArrayList<>(parts.size());
        for (QueryPart part : parts) {
            result.add(part.render(ctx));
        }
        return String.join(", ", result);
    }

    private static String joinFieldRefs(RenderContext ctx, Collection<Field<?>> fields) {
        List<String> result = new ArrayList<>(fields.size());
        for (Field<?> field : fields) {
            result.add(field.renderRef(ctx));
        }
        return String.join(", ", result);
    }
}
