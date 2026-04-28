package com.run.sql.dialect;

import com.run.sql.dialect.renderer.ConditionRenderer;
import com.run.sql.dialect.renderer.FunctionRenderer;
import com.run.sql.dialect.renderer.IdentifierRenderer;
import com.run.sql.dialect.renderer.JsonRenderer;
import com.run.sql.dialect.renderer.LimitRenderer;
import com.run.sql.dialect.renderer.LiteralRenderer;
import com.run.sql.dialect.renderer.StandardConditionRenderer;
import com.run.sql.dialect.renderer.StandardFunctionRenderer;
import com.run.sql.dialect.renderer.StandardIdentifierRenderer;
import com.run.sql.dialect.renderer.StandardLimitRenderer;
import com.run.sql.dialect.renderer.StandardLiteralRenderer;
import com.run.sql.dialect.renderer.UnsupportedJsonRenderer;

/** Generic dialect implementation. Useful for custom databases with standard syntax. */
public class StandardDialect extends AbstractDialect {

    public StandardDialect(String dialectName, String quoteStart, String quoteEnd, boolean quoteIdentifiersByDefault, IdentifierQualification qualification) {
        super(dialectName, quoteStart, quoteEnd, quoteIdentifiersByDefault, qualification, -1);
    }

    public StandardDialect(
            String dialectName,
            String quoteStart,
            String quoteEnd,
            boolean quoteIdentifiersByDefault,
            IdentifierQualification qualification,
            int maxInListSize,
            IdentifierRenderer identifiers,
            LimitRenderer limit,
            ConditionRenderer conditions,
            FunctionRenderer functions,
            LiteralRenderer literals,
            JsonRenderer json
    ) {
        super(dialectName, quoteStart, quoteEnd, quoteIdentifiersByDefault, qualification, maxInListSize,
                identifiers, limit, conditions, functions, literals, json);
    }

    public static StandardDialect schemaDialect(String name) {
        return new StandardDialect(name, "\"", "\"", true, IdentifierQualification.SCHEMA);
    }

    public static StandardDialect catalogDialect(String name) {
        return new StandardDialect(name, "\"", "\"", true, IdentifierQualification.CATALOG);
    }

    public static StandardDialect unquotedSchemaDialect(String name) {
        return new StandardDialect(name, "\"", "\"", false, IdentifierQualification.SCHEMA);
    }

    public static StandardDialect withDefaults(String name) {
        return new StandardDialect(name, "\"", "\"", true, IdentifierQualification.SCHEMA, -1,
                new StandardIdentifierRenderer(), new StandardLimitRenderer(), new StandardConditionRenderer(),
                new StandardFunctionRenderer(), new StandardLiteralRenderer(), new UnsupportedJsonRenderer());
    }
}
