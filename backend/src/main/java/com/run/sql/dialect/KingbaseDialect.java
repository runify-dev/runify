package com.run.sql.dialect;

import com.run.sql.dialect.renderer.PostgreSqlConditionRenderer;
import com.run.sql.dialect.renderer.PostgreSqlJsonRenderer;
import com.run.sql.dialect.renderer.StandardFunctionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;

/** Kingbase dialect. It is PostgreSQL-like for the syntax supported by this builder. */
public final class KingbaseDialect extends AbstractDialect {
    public KingbaseDialect() {
        super(
                "kingbase",
                "\"",
                "\"",
                true,
                IdentifierQualification.SCHEMA,
                -1,
                new StandardIdentifierRenderer(),
                new StandardLimitRenderer(),
                new PostgreSqlConditionRenderer(),
                new StandardFunctionRenderer(),
                new StandardLiteralRenderer(),
                new PostgreSqlJsonRenderer()
        );
    }
}
