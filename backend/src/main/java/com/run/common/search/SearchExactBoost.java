package com.run.common.search;

import java.util.Objects;

public final class SearchExactBoost {

    private final String field;
    private final String value;
    private final float boost;

    public SearchExactBoost(String field, String value, float boost) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (boost <= 0) {
            throw new IllegalArgumentException("boost must be > 0");
        }
        this.field = field;
        this.value = value;
        this.boost = boost;
    }

    public static SearchExactBoost of(String field, String value, float boost) {
        return new SearchExactBoost(field, value, boost);
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public float getBoost() {
        return boost;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchExactBoost that)) {
            return false;
        }
        return Float.compare(boost, that.boost) == 0
                && Objects.equals(field, that.field)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, value, boost);
    }
}
