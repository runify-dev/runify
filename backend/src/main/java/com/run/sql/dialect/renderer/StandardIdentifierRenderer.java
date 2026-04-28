package com.run.sql.dialect.renderer;

import com.run.sql.dialect.IdentifierQualification;
import com.run.sql.render.RenderContext;

import java.util.ArrayList;
import java.util.List;

/** Default identifier renderer. It quotes every identifier part when settings require quoting. */
public class StandardIdentifierRenderer implements IdentifierRenderer {

    @Override
    public String identifier(RenderContext ctx, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("identifier is blank");
        }
        String[] parts = name.split("\\.");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(renderPart(ctx, part));
        }
        return String.join(".", result);
    }

    @Override
    public String tableIdentifier(RenderContext ctx, String catalog, String schema, String name) {
        List<String> parts = new ArrayList<>(3);
        addQualifier(ctx, parts, catalog, schema);
        addIfNotBlank(parts, name);
        return joinIdentifiers(ctx, parts);
    }

    @Override
    public String fieldIdentifier(RenderContext ctx, String catalog, String schema, String table, String name) {
        List<String> parts = new ArrayList<>(4);
        if (table != null && !table.isBlank()) {
            addQualifier(ctx, parts, catalog, schema);
            addIfNotBlank(parts, table);
        }
        addIfNotBlank(parts, name);
        return joinIdentifiers(ctx, parts);
    }

    protected void addQualifier(RenderContext ctx, List<String> parts, String catalog, String schema) {
        IdentifierQualification qualification = ctx.dialect().qualification();
        if (qualification == IdentifierQualification.CATALOG) {
            addIfNotBlank(parts, catalog);
        } else if (qualification == IdentifierQualification.SCHEMA) {
            addIfNotBlank(parts, schema);
        }
    }

    protected String joinIdentifiers(RenderContext ctx, List<String> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("identifier parts is empty");
        }
        List<String> result = new ArrayList<>(parts.size());
        for (String part : parts) {
            result.add(renderPart(ctx, part));
        }
        return String.join(".", result);
    }

    protected String renderPart(RenderContext ctx, String part) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException("identifier part is blank");
        }
        if ("*".equals(part)) {
            return "*";
        }
        if (!ctx.settings().quoteIdentifiers(ctx.dialect())) {
            return part;
        }
        return ctx.quoteIdentifierPart(part);
    }

    protected static void addIfNotBlank(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }
}
