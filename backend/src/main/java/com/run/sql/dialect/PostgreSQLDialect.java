package com.run.sql.dialect;

import com.run.sql.dialect.renderer.PostgreSqlConditionRenderer;
import com.run.sql.dialect.renderer.PostgreSqlJsonRenderer;
import com.run.sql.dialect.renderer.StandardFunctionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;

/** PostgreSQL dialect. Uses schema qualification and quoted identifiers by default. */
public final class PostgreSQLDialect extends AbstractDialect {
    public PostgreSQLDialect() {
        super(
                "postgresql",
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
