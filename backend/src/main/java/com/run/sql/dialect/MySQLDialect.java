package com.run.sql.dialect;

import com.run.sql.dialect.renderer.MySqlConditionRenderer;
import com.run.sql.dialect.renderer.MySqlFunctionRenderer;
import com.run.sql.dialect.renderer.MySqlJsonRenderer;
import com.run.sql.dialect.renderer.MySqlLimitRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;

/** MySQL dialect. Uses catalog/database qualification and backtick identifiers. */
public final class MySQLDialect extends AbstractDialect {
    public MySQLDialect() {
        super(
                "mysql",
                "`",
                "`",
                true,
                IdentifierQualification.CATALOG,
                -1,
                new StandardIdentifierRenderer(),
                new MySqlLimitRenderer(),
                new MySqlConditionRenderer(),
                new MySqlFunctionRenderer(),
                new StandardLiteralRenderer(),
                new MySqlJsonRenderer()
        );
    }
}
