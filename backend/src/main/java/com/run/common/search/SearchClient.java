package com.run.common.search;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface SearchClient extends AutoCloseable {

    CompletionStage<Void> index(SearchDocument document);

    CompletionStage<Void> bulkIndex(Collection<SearchDocument> documents);

    CompletionStage<Void> updateById(String index, String id, Map<String, Object> fields);

    CompletionStage<Void> deleteById(String index, String id);

    CompletionStage<Void> deleteByIds(String index, Collection<String> ids);

    CompletionStage<Void> deleteByQuery(SearchQuery query);

    CompletionStage<Boolean> exists(String index, String id);

    CompletionStage<SearchResult<SearchDocument>> search(SearchRequest request);

    default CompletionStage<SearchResult<SearchDocument>> search(SearchQuery query) {
        return search(SearchRequest.from(query));
    }

    @Override
    void close() throws Exception;
}
