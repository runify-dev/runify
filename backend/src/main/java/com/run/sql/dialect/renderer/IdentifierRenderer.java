package com.run.sql.dialect.renderer;

import com.run.sql.render.RenderContext;

/** Renders SQL identifiers such as table names, field names and aliases. */
public interface IdentifierRenderer {
    String identifier(RenderContext ctx, String name);

    String tableIdentifier(RenderContext ctx, String catalog, String schema, String name);

    String fieldIdentifier(RenderContext ctx, String catalog, String schema, String table, String name);
}
