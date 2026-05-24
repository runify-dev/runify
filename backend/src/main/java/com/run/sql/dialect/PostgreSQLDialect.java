package com.run.sql.dialect;

import com.run.sql.dialect.renderer.PostgreSqlConditionRenderer;
import com.run.sql.dialect.renderer.PostgreSqlJsonRenderer;
import com.run.sql.dialect.renderer.StandardFunctionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    @Override
    public Object prepareBindValue(Object value) {
        if (value instanceof LocalDateTime dt) {
            return dt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
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
