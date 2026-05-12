package com.run.common.search;

import java.util.Objects;

public final class SearchSort {

    public enum Type {
        SCORE,
        STRING,
        INT,
        LONG,
        BOOLEAN
    }

    private final String field;
    private final Type type;
    private final boolean desc;

    public SearchSort(String field, Type type, boolean desc) {
        this.field = field;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.desc = desc;
    }

    public static SearchSort scoreDesc() {
        return new SearchSort("_score", Type.SCORE, true);
    }

    public static SearchSort scoreAsc() {
        return new SearchSort("_score", Type.SCORE, false);
    }

    public static SearchSort of(String field, Type type, boolean desc) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        return new SearchSort(field, type, desc);
    }

    public String getField() {
        return field;
    }

    public Type getType() {
        return type;
    }

    public boolean isDesc() {
        return desc;
    }
}
