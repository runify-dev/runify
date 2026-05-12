package com.run.common.search;

import java.util.Arrays;
import java.util.Objects;

public final class SearchKnn {

    private final String field;
    private final float[] vector;
    private final int k;
    private final Integer numCandidates;

    private SearchKnn(Builder builder) {
        if (builder.field == null || builder.field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (builder.vector == null || builder.vector.length == 0) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        if (builder.k < 1) {
            throw new IllegalArgumentException("k must be >= 1");
        }
        if (builder.numCandidates != null && builder.numCandidates < builder.k) {
            throw new IllegalArgumentException("numCandidates must be >= k");
        }
        this.field = builder.field;
        this.vector = Arrays.copyOf(builder.vector, builder.vector.length);
        this.k = builder.k;
        this.numCandidates = builder.numCandidates;
    }

    public static Builder builder(String field, float[] vector, int k) {
        return new Builder(field, vector, k);
    }

    public static SearchKnn of(String field, float[] vector, int k) {
        return builder(field, vector, k).build();
    }

    public String getField() {
        return field;
    }

    public float[] getVector() {
        return Arrays.copyOf(vector, vector.length);
    }

    public int getK() {
        return k;
    }

    public Integer getNumCandidates() {
        return numCandidates;
    }

    @Override
    public String toString() {
        return "SearchKnn{" +
                "field='" + field + '\'' +
                ", vectorLength=" + vector.length +
                ", k=" + k +
                ", numCandidates=" + numCandidates +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchKnn that)) {
            return false;
        }
        return k == that.k
                && Objects.equals(field, that.field)
                && Arrays.equals(vector, that.vector)
                && Objects.equals(numCandidates, that.numCandidates);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(field, k, numCandidates);
        result = 31 * result + Arrays.hashCode(vector);
        return result;
    }

    public static final class Builder {
        private final String field;
        private final float[] vector;
        private final int k;
        private Integer numCandidates;

        private Builder(String field, float[] vector, int k) {
            this.field = field;
            this.vector = vector == null ? null : Arrays.copyOf(vector, vector.length);
            this.k = k;
        }

        public Builder numCandidates(Integer numCandidates) {
            this.numCandidates = numCandidates;
            return this;
        }

        public SearchKnn build() {
            return new SearchKnn(this);
        }
    }
}
