package com.run.sql.dialect;

import com.run.sql.dialect.renderer.StandardConditionRenderer;
import com.run.sql.dialect.renderer.StandardFunctionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;
import com.run.sql.dialect.renderer.UnsupportedJsonRenderer;

/** H2 dialect. Uses schema qualification and quoted identifiers. */
public final class H2Dialect extends AbstractDialect {
    public H2Dialect() {
        super(
                "h2",
                "\"",
                "\"",
                true,
                IdentifierQualification.SCHEMA,
                -1,
                new StandardIdentifierRenderer(),
                new StandardLimitRenderer(),
                new StandardConditionRenderer(),
                new StandardFunctionRenderer(),
                new StandardLiteralRenderer(),
                new UnsupportedJsonRenderer()
        );
    }
}
