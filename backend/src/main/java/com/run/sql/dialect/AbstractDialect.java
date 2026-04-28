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

import java.util.Objects;

/** Base implementation for dialects. */
public abstract class AbstractDialect implements Dialect {

    private final String dialectName;
    private final String quoteStart;
    private final String quoteEnd;
    private final boolean quoteIdentifiersByDefault;
    private final IdentifierQualification qualification;
    private final int maxInListSize;
    private final IdentifierRenderer identifiers;
    private final LimitRenderer limit;
    private final ConditionRenderer conditions;
    private final FunctionRenderer functions;
    private final LiteralRenderer literals;
    private final JsonRenderer json;

    protected AbstractDialect(
            String dialectName,
            String quoteStart,
            String quoteEnd,
            boolean quoteIdentifiersByDefault,
            IdentifierQualification qualification,
            int maxInListSize
    ) {
        this(
                dialectName,
                quoteStart,
                quoteEnd,
                quoteIdentifiersByDefault,
                qualification,
                maxInListSize,
                new StandardIdentifierRenderer(),
                new StandardLimitRenderer(),
                new StandardConditionRenderer(),
                new StandardFunctionRenderer(),
                new StandardLiteralRenderer(),
                new UnsupportedJsonRenderer()
        );
    }

    protected AbstractDialect(
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
        this.dialectName = Objects.requireNonNull(dialectName, "dialectName");
        this.quoteStart = Objects.requireNonNull(quoteStart, "quoteStart");
        this.quoteEnd = Objects.requireNonNull(quoteEnd, "quoteEnd");
        this.quoteIdentifiersByDefault = quoteIdentifiersByDefault;
        this.qualification = Objects.requireNonNull(qualification, "qualification");
        this.maxInListSize = maxInListSize;
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
        this.limit = Objects.requireNonNull(limit, "limit");
        this.conditions = Objects.requireNonNull(conditions, "conditions");
        this.functions = Objects.requireNonNull(functions, "functions");
        this.literals = Objects.requireNonNull(literals, "literals");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public String dialectName() {
        return dialectName;
    }

    @Override
    public String quoteStart() {
        return quoteStart;
    }

    @Override
    public String quoteEnd() {
        return quoteEnd;
    }

    @Override
    public boolean quoteIdentifiersByDefault() {
        return quoteIdentifiersByDefault;
    }

    @Override
    public IdentifierQualification qualification() {
        return qualification;
    }

    @Override
    public int maxInListSize() {
        return maxInListSize;
    }

    @Override
    public IdentifierRenderer identifiers() {
        return identifiers;
    }

    @Override
    public LimitRenderer limit() {
        return limit;
    }

    @Override
    public ConditionRenderer conditions() {
        return conditions;
    }

    @Override
    public FunctionRenderer functions() {
        return functions;
    }

    @Override
    public LiteralRenderer literals() {
        return literals;
    }

    @Override
    public JsonRenderer json() {
        return json;
    }
}
