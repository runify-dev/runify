package com.run.sql.dialect;

import com.run.sql.dialect.renderer.NumericBooleanLiteralRenderer;
import com.run.sql.dialect.renderer.SQLiteFunctionRenderer;
import com.run.sql.dialect.renderer.SQLiteJsonRenderer;
import com.run.sql.dialect.renderer.StandardConditionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;

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
                new NumericBooleanLiteralRenderer(),
                new SQLiteJsonRenderer()
        );
    }

    @Override
    public Object prepareBindValue(Object value) {
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (value instanceof LocalDateTime dt) {
            return dt.toString();
        }
        if (value instanceof JsonArray a) {
            return a.encode();
        }
        if (value instanceof JsonObject o) {
            return o.encode();
        }
        return value;
    }
}
