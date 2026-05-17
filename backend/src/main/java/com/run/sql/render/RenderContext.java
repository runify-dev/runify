package com.run.sql.render;

import com.run.sql.dialect.Dialect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class RenderContext {

    private final Dialect dialect;
    private final RenderSettings settings;
    private final Map<String, Object> params = new LinkedHashMap<>();
    private final AtomicInteger index = new AtomicInteger();

    public RenderContext(Dialect dialect, RenderSettings settings) {
        this.dialect = Objects.requireNonNull(dialect, "dialect");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public Dialect dialect() {
        return dialect;
    }

    public RenderSettings settings() {
        return settings;
    }

    public Map<String, Object> params() {
        return params;
    }

    public String bind(String preferredName, Object value) {
        String base = normalizeParamName(
                preferredName == null || preferredName.isBlank()
                        ? "p" + index.incrementAndGet()
                        : preferredName
        );

        Object prepared = dialect.prepareBindValue(value);

        String name = base;
        if (params.containsKey(name)) {
            Object oldValue = params.get(name);
            if (Objects.equals(oldValue, prepared)) {
                return "#{" + name + "}";
            }

            int i = 1;
            do {
                name = base + "_" + i++;
            } while (params.containsKey(name));
        }

        params.put(name, prepared);
        return "#{" + name + "}";
    }

    public void putNamedParam(String name, Object value) {
        String normalized = normalizeParamName(name);
        Object prepared = dialect.prepareBindValue(value);
        if (params.containsKey(normalized) && !Objects.equals(params.get(normalized), prepared)) {
            throw new IllegalArgumentException("重复参数名但值不同: " + normalized);
        }
        params.put(normalized, prepared);
    }

    public String paramTemplate(String name) {
        return "#{" + normalizeParamName(name) + "}";
    }

    /**
     * Register a named parameter without binding a value.
     * The actual value will be provided at execution time via the params map.
     */
    public void registerParamName(String name) {
        String normalized = normalizeParamName(name);
        params.putIfAbsent(normalized, null);
    }

    public String identifier(String name) {
        return dialect.identifiers().identifier(this, name);
    }

    public String tableIdentifier(String catalog, String schema, String name) {
        return dialect.identifiers().tableIdentifier(this, catalog, schema, name);
    }

    public String fieldIdentifier(String catalog, String schema, String table, String name) {
        return dialect.identifiers().fieldIdentifier(this, catalog, schema, table, name);
    }

    public String quoteIdentifierPart(String part) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException("identifier part is blank");
        }
        String end = dialect.quoteEnd();
        String escaped = part.replace(end, end + end);
        return dialect.quoteStart() + escaped + end;
    }

    private static String normalizeParamName(String name) {
        if (name == null || name.isBlank()) {
            return "p";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (Character.isJavaIdentifierStart(c)) {
                    sb.append(c);
                } else {
                    sb.append('p');
                    sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
                }
            } else {
                sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }

        return sb.toString();
    }
}
