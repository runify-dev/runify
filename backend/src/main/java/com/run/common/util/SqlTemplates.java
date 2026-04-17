package com.run.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SqlTemplates {

    private SqlTemplates() {
    }

    public static ExpandedSql expandIn(String sql, String paramName, List<?> values) {
        if (values == null || values.isEmpty()) {
            // SQLite 允许空 IN 列表，但这里更稳一点，直接改成永远不命中
            String replaced = sql.replace("#{" + paramName + "}", "NULL");
            return new ExpandedSql(replaced, new HashMap<>());
        }

        Map<String, Object> params = new HashMap<>();
        String joined = IntStream.range(0, values.size())
                .mapToObj(i -> {
                    String key = paramName + "_" + i;
                    params.put(key, values.get(i));
                    return "#{" + key + "}";
                })
                .collect(Collectors.joining(", "));

        String replaced = sql.replace("#{" + paramName + "}", joined);
        return new ExpandedSql(replaced, params);
    }

    public static final class ExpandedSql {
        private final String sql;
        private final Map<String, Object> params;

        public ExpandedSql(String sql, Map<String, Object> params) {
            this.sql = sql;
            this.params = params;
        }

        public String sql() {
            return sql;
        }

        public Map<String, Object> params() {
            return params;
        }
    }
}