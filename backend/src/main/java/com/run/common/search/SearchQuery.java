package com.run.common.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SearchQuery {

    private final String index;
    private final String keyword;
    private final List<String> keywordFields;
    private final Map<String, List<String>> exactFilters;
    private final List<String> ids;
    private final int pageNo;
    private final int pageSize;
    private final Map<String, Object> options;

    private SearchQuery(Builder builder) {
        if (builder.index == null || builder.index.isBlank()) {
            throw new IllegalArgumentException("index must not be blank");
        }
        if (builder.pageNo < 1) {
            throw new IllegalArgumentException("pageNo must be >= 1");
        }
        if (builder.pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        this.index = builder.index;
        this.keyword = builder.keyword;
        this.keywordFields = Collections.unmodifiableList(new ArrayList<>(builder.keywordFields));

        Map<String, List<String>> filterCopy = new LinkedHashMap<>();
        builder.exactFilters.forEach((k, v) -> filterCopy.put(k, List.copyOf(v)));
        this.exactFilters = Collections.unmodifiableMap(filterCopy);

        this.ids = Collections.unmodifiableList(new ArrayList<>(builder.ids));
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<>(builder.options));
    }

    public static Builder builder(String index) {
        return new Builder(index);
    }

    public String getIndex() {
        return index;
    }

    public String getKeyword() {
        return keyword;
    }

    public List<String> getKeywordFields() {
        return keywordFields;
    }

    public Map<String, List<String>> getExactFilters() {
        return exactFilters;
    }

    public List<String> getIds() {
        return ids;
    }

    public int getPageNo() {
        return pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getFrom() {
        return (pageNo - 1) * pageSize;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public static final class Builder {
        private final String index;
        private String keyword;
        private final List<String> keywordFields = new ArrayList<>();
        private final Map<String, List<String>> exactFilters = new LinkedHashMap<>();
        private final List<String> ids = new ArrayList<>();
        private int pageNo = 1;
        private int pageSize = 10;
        private final Map<String, Object> options = new LinkedHashMap<>();

        private Builder(String index) {
            this.index = index;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder keywordFields(String... fields) {
            if (fields != null) {
                for (String field : fields) {
                    if (field != null && !field.isBlank()) {
                        this.keywordFields.add(field);
                    }
                }
            }
            return this;
        }

        public Builder keywordFields(Collection<String> fields) {
            if (fields != null) {
                for (String field : fields) {
                    if (field != null && !field.isBlank()) {
                        this.keywordFields.add(field);
                    }
                }
            }
            return this;
        }

        public Builder exactFilter(String field, String... values) {
            if (field == null || field.isBlank() || values == null || values.length == 0) {
                return this;
            }
            List<String> list = this.exactFilters.computeIfAbsent(field, k -> new ArrayList<>());
            for (String value : values) {
                if (value != null) {
                    list.add(value);
                }
            }
            return this;
        }

        public Builder exactFilter(String field, Collection<String> values) {
            if (field == null || field.isBlank() || values == null || values.isEmpty()) {
                return this;
            }
            List<String> list = this.exactFilters.computeIfAbsent(field, k -> new ArrayList<>());
            for (String value : values) {
                if (value != null) {
                    list.add(value);
                }
            }
            return this;
        }

        public Builder ids(String... ids) {
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) {
                        this.ids.add(id);
                    }
                }
            }
            return this;
        }

        public Builder ids(Collection<String> ids) {
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) {
                        this.ids.add(id);
                    }
                }
            }
            return this;
        }

        public Builder pageNo(int pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder option(String key, Object value) {
            if (key != null && !key.isBlank()) {
                this.options.put(key, value);
            }
            return this;
        }

        public SearchQuery build() {
            return new SearchQuery(this);
        }
    }
}
