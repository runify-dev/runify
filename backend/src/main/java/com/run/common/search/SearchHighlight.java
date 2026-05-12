package com.run.common.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SearchHighlight {

    private final List<String> fields;
    private final String preTag;
    private final String postTag;
    private final int fragmentSize;

    public SearchHighlight(List<String> fields, String preTag, String postTag, int fragmentSize) {
        this.fields = fields == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(fields));
        this.preTag = preTag == null ? "<em>" : preTag;
        this.postTag = postTag == null ? "</em>" : postTag;
        this.fragmentSize = fragmentSize <= 0 ? 120 : fragmentSize;
    }

    public static SearchHighlight of(Collection<String> fields) {
        return new SearchHighlight(fields == null ? List.of() : new ArrayList<>(fields), "<em>", "</em>", 120);
    }

    public List<String> getFields() {
        return fields;
    }

    public String getPreTag() {
        return preTag;
    }

    public String getPostTag() {
        return postTag;
    }

    public int getFragmentSize() {
        return fragmentSize;
    }
}
