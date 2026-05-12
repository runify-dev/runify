package com.run.common.search;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SearchResult<T> {

    private final long total;
    private final int pageNo;
    private final int pageSize;
    private final List<SearchHit<T>> hits;

    public SearchResult(long total, int pageNo, int pageSize, List<SearchHit<T>> hits) {
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.hits = hits == null ? List.of() : Collections.unmodifiableList(hits);
    }

    public long getTotal() {
        return total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<SearchHit<T>> getHits() {
        return hits;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "total=" + total +
                ", pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                ", hits=" + hits +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SearchResult<?> that)) {
            return false;
        }
        return total == that.total
                && pageNo == that.pageNo
                && pageSize == that.pageSize
                && Objects.equals(hits, that.hits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, pageNo, pageSize, hits);
    }
}
