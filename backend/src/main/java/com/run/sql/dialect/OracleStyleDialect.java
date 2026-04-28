package com.run.sql.dialect;

import com.run.sql.dialect.renderer.NumericBooleanLiteralRenderer;
import com.run.sql.dialect.renderer.OracleFunctionRenderer;
import com.run.sql.dialect.renderer.OracleJsonRenderer;
import com.run.sql.dialect.renderer.OracleLimitRenderer;
import com.run.sql.dialect.renderer.StandardConditionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;

/** Oracle/DM style dialect. Uses schema qualification and avoids quoting by default. */
public final class OracleStyleDialect extends AbstractDialect {
    public OracleStyleDialect(String dialectName) {
        super(
                dialectName,
                "\"",
                "\"",
                false,
                IdentifierQualification.SCHEMA,
                1000,
                new StandardIdentifierRenderer(),
                new OracleLimitRenderer(),
                new StandardConditionRenderer(),
                new OracleFunctionRenderer(),
                new NumericBooleanLiteralRenderer(),
                new OracleJsonRenderer()
        );
    }
}
