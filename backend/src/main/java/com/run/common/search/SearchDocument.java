package com.run.common.search;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SearchDocument {

    private final String index;
    private final String id;
    private final Map<String, Object> fields;

    public SearchDocument(String index, String id, Map<String, Object> fields) {
        if (index == null || index.isBlank()) {
            throw new IllegalArgumentException("index must not be blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.index = index;
        this.id = id;
        this.fields = fields == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return "SearchDocument{" +
                "index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", fields=" + fields +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchDocument that)) {
            return false;
        }
        return Objects.equals(index, that.index)
                && Objects.equals(id, that.id)
                && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, id, fields);
    }
}
