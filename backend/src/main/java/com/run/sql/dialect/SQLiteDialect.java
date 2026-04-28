package com.run.sql.dialect;

import com.run.sql.dialect.renderer.SQLiteFunctionRenderer;
import com.run.sql.dialect.renderer.SQLiteJsonRenderer;
import com.run.sql.dialect.renderer.StandardConditionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;

/** SQLite dialect. Uses attached database/catalog qualification and quoted identifiers. */
public final class SQLiteDialect extends AbstractDialect {
    public SQLiteDialect() {
        super(
                "sqlite",
                "\"",
                "\"",
                true,
                IdentifierQualification.CATALOG,
                -1,
                new StandardIdentifierRenderer(),
                new StandardLimitRenderer(),
                new StandardConditionRenderer(),
                new SQLiteFunctionRenderer(),
                new StandardLiteralRenderer(),
                new SQLiteJsonRenderer()
        );
    }
}
