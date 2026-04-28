package com.run.sql.dialect;

import com.run.sql.dialect.renderer.ConditionRenderer;
import com.run.sql.dialect.renderer.FunctionRenderer;
import com.run.sql.dialect.renderer.IdentifierRenderer;
import com.run.sql.dialect.renderer.JsonRenderer;
import com.run.sql.dialect.renderer.LimitRenderer;
import com.run.sql.dialect.renderer.LiteralRenderer;

/**
 * Built-in dialect enum kept for jOOQ-like usage: {@code DSL.using(SQLDialect.MYSQL)}.
 *
 * <p>For external dialects, implement {@link Dialect} directly and pass it to
 * {@code DSL.using(new YourDialect())}; no need to modify this enum.</p>
 */
public enum SQLDialect implements Dialect {
    MYSQL(BuiltInDialects.MYSQL),
    POSTGRESQL(BuiltInDialects.POSTGRESQL),
    KINGBASE(BuiltInDialects.KINGBASE),
    SQLITE(BuiltInDialects.SQLITE),
    H2(BuiltInDialects.H2),
    ORACLE(BuiltInDialects.ORACLE),
    DM(BuiltInDialects.DM);

    private final Dialect delegate;

    SQLDialect(Dialect delegate) {
        this.delegate = delegate;
    }

    public Dialect delegate() {
        return delegate;
    }

    @Override
    public String dialectName() {
        return delegate.dialectName();
    }

    @Override
    public String quoteStart() {
        return delegate.quoteStart();
    }

    @Override
    public String quoteEnd() {
        return delegate.quoteEnd();
    }

    @Override
    public boolean quoteIdentifiersByDefault() {
        return delegate.quoteIdentifiersByDefault();
    }

    @Override
    public IdentifierQualification qualification() {
        return delegate.qualification();
    }

    @Override
    public int maxInListSize() {
        return delegate.maxInListSize();
    }

    @Override
    public IdentifierRenderer identifiers() {
        return delegate.identifiers();
    }

    @Override
    public LimitRenderer limit() {
        return delegate.limit();
    }

    @Override
    public ConditionRenderer conditions() {
        return delegate.conditions();
    }

    @Override
    public FunctionRenderer functions() {
        return delegate.functions();
    }

    @Override
    public LiteralRenderer literals() {
        return delegate.literals();
    }

    @Override
    public JsonRenderer json() {
        return delegate.json();
    }
}
