package com.run.sql.dialect;

import com.run.sql.dialect.renderer.ConditionRenderer;
import com.run.sql.dialect.renderer.FunctionRenderer;
import com.run.sql.dialect.renderer.IdentifierRenderer;
import com.run.sql.dialect.renderer.JsonRenderer;
import com.run.sql.dialect.renderer.LimitRenderer;
import com.run.sql.dialect.renderer.LiteralRenderer;
import com.run.sql.render.RenderContext;

/**
 * Database dialect SPI.
 *
 * <p>The query model expresses what you want: compare, regex, concat, jsonText, pagination.
 * Dialect renderers decide how that concept is written for a specific database.</p>
 */
public interface Dialect {

    /** Stable name used by registry, logs and diagnostics, e.g. mysql, postgresql. */
    String dialectName();

    /** Quote prefix for identifiers. MySQL uses backtick, most SQL databases use double quote. */
    String quoteStart();

    /** Quote suffix for identifiers. */
    String quoteEnd();

    /** Whether identifiers are quoted by default. */
    boolean quoteIdentifiersByDefault();

    /** Which qualifier should be used for this database. */
    IdentifierQualification qualification();

    /** Max item count for a single IN list. Return <= 0 for unlimited. */
    default int maxInListSize() {
        return -1;
    }

    IdentifierRenderer identifiers();

    LimitRenderer limit();

    ConditionRenderer conditions();

    FunctionRenderer functions();

    LiteralRenderer literals();

    JsonRenderer json();

    /** Compatibility method for old code. Prefer {@code dialect.limit().render(...)}. */
    default String renderLimitOffset(String sql, RenderContext ctx, Long limit, Long offset) {
        return limit().render(ctx, sql, limit, offset);
    }

    /**
     * Transform a bind parameter value for this database before it is sent to the driver.
     * <p>Default returns the value unchanged. Databases without native boolean
     * (e.g. SQLite) can override to convert {@code Boolean} to {@code Integer}.</p>
     */
    default Object prepareBindValue(Object value) {
        return value;
    }
}
