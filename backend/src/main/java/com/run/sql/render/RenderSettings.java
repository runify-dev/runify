package com.run.sql.render;

import com.run.sql.dialect.Dialect;

public final class RenderSettings {

    /**
     * null 表示使用方言默认值。
     */
    private final Boolean quoteIdentifiers;

    private RenderSettings(Boolean quoteIdentifiers) {
        this.quoteIdentifiers = quoteIdentifiers;
    }

    public static RenderSettings defaults() {
        return new RenderSettings(null);
    }

    public static RenderSettings quotedIdentifiers() {
        return new RenderSettings(true);
    }

    public static RenderSettings unquotedIdentifiers() {
        return new RenderSettings(false);
    }

    public boolean quoteIdentifiers(Dialect dialect) {
        return quoteIdentifiers == null ? dialect.quoteIdentifiersByDefault() : quoteIdentifiers;
    }
}
