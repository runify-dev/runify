package com.run.common.search;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;

import java.nio.file.Path;
import java.util.List;

public final class SearchClients {

    private SearchClients() {
    }

    public static SearchClient lucene(
            Path path,
            Analyzer analyzer,
            ObjectMapper objectMapper,
            List<String> defaultSearchFields
    ) {
        try {
            return new LuceneSearchClient(path, analyzer, objectMapper, defaultSearchFields);
        } catch (Exception e) {
            throw new SearchException("failed to create lucene search client", e);
        }
    }

    public static SearchClient elasticsearch(
            ElasticsearchAsyncClient client,
            ObjectMapper objectMapper,
            List<String> defaultSearchFields
    ) {
        return new ElasticsearchSearchClient(client, objectMapper, defaultSearchFields);
    }
}
