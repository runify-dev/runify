package com.run.common.search;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SearchHit<T> {

    private final String index;
    private final String id;
    private final float score;
    private final T source;
    private final Map<String, List<String>> highlights;

    public SearchHit(String index, String id, float score, T source) {
        this(index, id, score, source, Map.of());
    }

    public SearchHit(String index, String id, float score, T source, Map<String, List<String>> highlights) {
        this.index = index;
        this.id = id;
        this.score = score;
        this.source = source;
        this.highlights = highlights == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(highlights));
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    public T getSource() {
        return source;
    }

    public Map<String, List<String>> getHighlights() {
        return highlights;
    }

    @Override
    public String toString() {
        return "SearchHit{" +
                "index='" + index + '\'' +
                ", id='" + id + '\'' +
                ", score=" + score +
                ", source=" + source +
                ", highlights=" + highlights +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchHit<?> that)) {
            return false;
        }
        return Float.compare(score, that.score) == 0
                && Objects.equals(index, that.index)
                && Objects.equals(id, that.id)
                && Objects.equals(source, that.source)
                && Objects.equals(highlights, that.highlights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, id, score, source, highlights);
    }
}
