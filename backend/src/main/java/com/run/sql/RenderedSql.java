package com.run.sql;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RenderedSql(String sql, Map<String, Object> params) {
    public RenderedSql {
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
