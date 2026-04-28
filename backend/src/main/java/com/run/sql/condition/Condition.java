package com.run.sql.condition;

import com.run.sql.QueryPart;

public interface Condition extends QueryPart {
    default Condition and(Condition other) {
        if (this.isEmpty()) {
            return other;
        }
        if (other == null || other.isEmpty()) {
            return this;
        }
        return new LogicalCondition(this, "and", other);
    }

    default Condition or(Condition other) {
        if (this.isEmpty()) {
            return other;
        }
        if (other == null || other.isEmpty()) {
            return this;
        }
        return new LogicalCondition(this, "or", other);
    }

    default boolean isEmpty() {
        return false;
    }
}
